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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * FTP file downloader
 * Support resume download from break point
 */
public class FTPUtils {
    private final Logger logger = LoggerFactory.getLogger(FTPUtils.class);
    @Inject private FTPClient ftpClient;

    public enum DownloadStatus {
        Remote_File_Not_Exist,
        Local_Bigger_Remote,
        Download_From_Break_Success,
        Download_From_Break_Failed,
        Download_New_Success,
        Download_New_Failed
    }

    public boolean connect() throws IOException {
        FTPConfig ftpConfig = this.loadFTPConfig();
        ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
        ftpClient.connect(ftpConfig.getHost(), ftpConfig.getPort());
        ftpClient.setControlEncoding("UTF8");
        if (FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
            if (ftpClient.login(ftpConfig.getUsername(), ftpConfig.getPwd())) {
                if (ftpConfig.isActiveMode()) {
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

    public DownloadStatus download(String remote, String local) throws IOException {
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        DownloadStatus result;

        FTPFile[] files = ftpClient.listFiles(new String(remote.getBytes(StandardCharsets.UTF_8)));
        if (files.length != 1) {
            logger.error("Remote file not exists");
            return DownloadStatus.Remote_File_Not_Exist;
        }

        long lRemoteSize = files[0].getSize();
        File f = new File(local);

        // Local file exists already, resume downloading from break point
        if (f.exists()) {
            long localSize = f.length();
            if (localSize >= lRemoteSize) {
                logger.warn("Local file larger than remote file, stop downloading.");
                return DownloadStatus.Local_Bigger_Remote;
            }

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
                    logger.info("Download progress: " + process);
                }
            }
        }
        in.close();
        out.close();
        return ftpClient.completePendingCommand();
    }

    private void disconnect() throws IOException {
        if (ftpClient.isConnected()) {
            ftpClient.disconnect();
        }
    }

    public void run(String remoteFilePath, String localFilePath) {
        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            try {
                this.connect();
                this.download(remoteFilePath, localFilePath);

                File localFile = FileUtils.getFile(localFilePath);
                File backupFile = FileUtils.getFile(localFile.getParent(), "backup-" + localFile.getName());
                if (backupFile.exists()) {
                    FileUtils.deleteQuietly(backupFile);
                }
                FileUtils.moveFile(localFile, backupFile);
                this.disconnect();
                break;
            } catch (IOException e) {
                logger.error("FTP connection error：", e);
            }
        }
    }
}