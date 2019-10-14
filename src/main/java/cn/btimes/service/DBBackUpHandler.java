package cn.btimes.service;

import cn.btimes.model.common.Config;
import cn.btimes.utils.FTPUtils;
import com.google.inject.Inject;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/10/14 20:30
 */
public class DBBackUpHandler implements ServiceExecutorInterface {
    @Inject private FTPUtils ftpUtils;

    @Override
    public void execute(Config config) {
        ftpUtils.run();
    }
}
