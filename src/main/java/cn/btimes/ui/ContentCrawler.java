package cn.btimes.ui;

import cn.btimes.model.common.Application;
import cn.btimes.model.common.Config;
import cn.btimes.utils.Common;
import com.amzass.utils.common.ProcessCleaner;
import com.amzass.utils.common.Tools;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 12/24/2018 5:35 AM
 */
public class ContentCrawler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentCrawler.class);

    public static void main(String[] args) {
        ProcessCleaner.cleanWebDriver();
        if (ArrayUtils.contains(args, "CLEAN_DRIVERS_ONLY")) {
            System.exit(0);
        }
        LOGGER.info("Start crawling contents.");
        long totalStart = System.currentTimeMillis();
        String appName = args.length > 0 ? args[0] : null;
        Application application = Application.determineApplication(appName);

        LOGGER.info("Running application: {}", application.name());
        Config config = Common.loadApplicationConfig(application);
        try {
            application.executor.execute(config);
        } catch (Exception e) {
            LOGGER.error("Unknown error found: ", e);
        } finally {
            LOGGER.info("Total execute time: {}", Tools.formatCostTime(totalStart));
            ProcessCleaner.cleanWebDriver();
            System.exit(0);
        }
    }
}
