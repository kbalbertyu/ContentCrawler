package cn.btimes.utils;

import cn.btimes.model.common.FTPConfig;
import com.alibaba.fastjson.JSONObject;
import com.amzass.enums.common.Directory;
import com.amzass.utils.common.Constants;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.google.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 支持断点续传的FTP实用类
 *
 * @version 0.3 实现中文目录创建及中文文件创建，添加对于中文的支持
 */
public class FTPUtils {
    private static final String DB_FILE_NAME = "db.sql.gz";
    private static final String DB_BACKUP_FILE_NAME = "db_bk.sql.gz";
    private static final String DOWNLOAD_PATH = "downloads";
    @Inject private FTPClient ftpClient;

    /**
     * 枚举类DownloadStatus代码
     */
    public enum DownloadStatus {
        Remote_File_Not_Exist, //远程文件不存在   
        Local_Bigger_Remote, //本地文件大于远程文件   
        Download_From_Break_Success, //断点下载文件成功   
        Download_From_Break_Failed,   //断点下载文件失败   
        Download_New_Success,    //全新下载文件成功   
        Download_New_Failed    //全新下载文件失败
    }

    /**
     * 连接到FTP服务器
     *
     * @return 是否连接成功
     */
    public boolean connect() throws IOException {
        FTPConfig ftpConfig = this.loadFTPConfig();
        ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
        ftpClient.connect(ftpConfig.getHost(), ftpConfig.getPort());
        ftpClient.setControlEncoding("UTF8");
        if (FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
            if (ftpClient.login(ftpConfig.getUsername(), ftpConfig.getPwd())) {
                if (ftpConfig.isActiveMode()) {
                    //设置被动模式
                    ftpClient.enterLocalActiveMode();
                } else {
                    ftpClient.enterLocalPassiveMode();
                }
                return true;
            }
        }
        disconnect();
        return false;
    }

    private FTPConfig loadFTPConfig() {
        String configFilePath = String.format("%s/%s", Directory.Customize.path(), "ftpConfig.json");
        File file = FileUtils.getFile(configFilePath);
        if (!file.exists()) {
            throw new BusinessException("FTP config file not exists: " + configFilePath);
        }
        return JSONObject.parseObject(Tools.readFileToString(file), FTPConfig.class);
    }

    /**
     * 从FTP服务器上下载文件,支持断点续传，上传百分比汇报
     *
     * @param remote 远程文件路径
     * @param local  本地文件路径
     * @return 上传的状态
     */
    public DownloadStatus download(String remote, String local) throws IOException {
        //设置以二进制方式传输     
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        DownloadStatus result;

        //检查远程文件是否存在     
        FTPFile[] files = ftpClient.listFiles(new String(remote.getBytes(StandardCharsets.UTF_8)));
        if (files.length != 1) {
            System.out.println("远程文件不存在");
            return DownloadStatus.Remote_File_Not_Exist;
        }

        long lRemoteSize = files[0].getSize();
        File f = new File(local);

        //本地存在文件，进行断点下载     
        if (f.exists()) {
            long localSize = f.length();
            //判断本地文件大小是否大于远程文件大小     
            if (localSize >= lRemoteSize) {
                System.out.println("本地文件大于远程文件，下载中止");
                return DownloadStatus.Local_Bigger_Remote;
            }

            //进行断点续传，并记录状态     
            FileOutputStream out = new FileOutputStream(f, true);
            ftpClient.setRestartOffset(localSize);
            InputStream in = ftpClient.retrieveFileStream(new String(remote.getBytes(StandardCharsets.UTF_8)));
            byte[] bytes = new byte[1024];
            long step = lRemoteSize / 100;
            long process = localSize / step;
            boolean download = this.download(localSize, out, in, bytes, step, process);
            if (download) {
                result = DownloadStatus.Download_From_Break_Success;
            } else {
                result = DownloadStatus.Download_From_Break_Failed;
            }
        } else {
            OutputStream out = new FileOutputStream(f);
            InputStream in = ftpClient.retrieveFileStream(new String(remote.getBytes(StandardCharsets.UTF_8)));
            byte[] bytes = new byte[1024];
            long step = lRemoteSize / 100;
            long process = 0;
            long localSize = 0L;
            boolean download = this.download(localSize, out, in, bytes, step, process);
            if (download) {
                result = DownloadStatus.Download_New_Success;
            } else {
                result = DownloadStatus.Download_New_Failed;
            }
        }
        return result;
    }

    private <T extends OutputStream> boolean download(long localSize, T out, InputStream in, byte[] bytes, long step, long process) throws IOException {
        int c;
        while ((c = in.read(bytes)) != -1) {
            out.write(bytes, 0, c);
            localSize += c;
            long nowProcess = localSize / step;
            if (nowProcess > process) {
                process = nowProcess;
                if (process % 10 == 0) {
                    System.out.println("下载进度：" + process);
                }
            }
        }
        in.close();
        out.close();
        return ftpClient.completePendingCommand();
    }

    /**
     * 断开与远程服务器的连接
     */
    private void disconnect() throws IOException {
        if (ftpClient.isConnected()) {
            ftpClient.disconnect();
        }
    }

    public void run() {
        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            try {
                this.connect();
                this.download(DB_FILE_NAME, DOWNLOAD_PATH + "/" + DB_FILE_NAME);
                File backFile = FileUtils.getFile(DOWNLOAD_PATH, DB_BACKUP_FILE_NAME);
                if (backFile.exists()) {
                    FileUtils.deleteQuietly(backFile);
                }
                File file = FileUtils.getFile(DOWNLOAD_PATH, DB_FILE_NAME);
                FileUtils.copyFile(file, backFile);
                this.disconnect();
                break;
            } catch (IOException e) {
                System.out.println("连接FTP出错：" + e.getMessage());
            }
        }
    }
}