package cn.btimes.service;

import cn.btimes.model.common.Config;
import cn.btimes.utils.FTPUtils;
import com.google.inject.Inject;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/10/14 20:30
 */
public class DBBackUpHandler implements ServiceExecutorInterface {
    private static final String DB_FILE_NAME = "db.sql.gz";
    private static final String DOWNLOAD_PATH = "downloads";
    @Inject private FTPUtils ftpUtils;

    @Override
    public void execute(Config config) {
        ftpUtils.run(DB_FILE_NAME, DOWNLOAD_PATH + "/" + DB_FILE_NAME);
    }
}
