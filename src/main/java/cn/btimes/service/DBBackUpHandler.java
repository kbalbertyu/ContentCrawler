package cn.btimes.service;

import cn.btimes.model.common.Config;
import cn.btimes.utils.FTPUtils;
import com.google.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/10/14 20:30
 */
public class DBBackUpHandler implements ServiceExecutorInterface {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String DB_FILE_NAME = "db.sql.gz";
    private static final String DOWNLOAD_PATH = "downloads";
    @Inject private FTPUtils ftpUtils;

    @Override
    public void execute(Config config) {
        ftpUtils.run(DB_FILE_NAME, DOWNLOAD_PATH + "/" + DB_FILE_NAME);

        File localFile = FileUtils.getFile(DOWNLOAD_PATH, "backup-" + DB_FILE_NAME);
        File toFile = FileUtils.getFile("C:/Work/Apps/BT", DB_FILE_NAME);
        try {
            FileUtils.copyFile(localFile, toFile);
        } catch (IOException e) {
            logger.error("Unable to copy file", e);
        }
    }
}
