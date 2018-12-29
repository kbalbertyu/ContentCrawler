package cn.btimes.ui;

import cn.btimes.service.ServiceExecutor;
import com.amzass.service.common.ApplicationContext;
import com.amzass.utils.common.ProcessCleaner;
import com.amzass.utils.common.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 12/24/2018 5:35 AM
 */
public class ContentCrawler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentCrawler.class);

    public static void main(String[] args) {
        try {
            long totalStart = System.currentTimeMillis();
            ApplicationContext.getBean(ServiceExecutor.class).execute();
            LOGGER.info("Total execute time: {}", Tools.formatCostTime(totalStart));
        } finally {
            ProcessCleaner.cleanWebDriver();
            System.exit(0);
        }
    }
}
