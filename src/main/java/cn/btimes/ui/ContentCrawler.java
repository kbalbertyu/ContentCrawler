package cn.btimes.ui;

import cn.btimes.model.common.Config;
import cn.btimes.service.ServiceExecutor;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.amzass.enums.common.Directory;
import com.amzass.service.common.ApplicationContext;
import com.amzass.utils.common.ProcessCleaner;
import com.amzass.utils.common.Tools;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 12/24/2018 5:35 AM
 */
public class ContentCrawler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentCrawler.class);
    public enum Application {
        BTimes(ApplicationContext.getBean(ServiceExecutor.class)),
        Kpopstarz(ApplicationContext.getBean(cn.kpopstarz.service.ServiceExecutor.class));

        Application(ServiceExecutor executor) {
            this.executor = executor;
        }

        private static Application determineApplication(String name) {
            if (StringUtils.isBlank(name)) {
                return BTimes;
            }
            for (Application application : Application.values()) {
                if (StringUtils.equalsIgnoreCase(application.name(), name)) {
                    return application;
                }
            }
            return BTimes;
        }

        private ServiceExecutor executor;
    }

    private static Config loadApplicationConfig(Application application) {
        String configStr = Tools.readFileToString(FileUtils.getFile(Directory.Customize.path(), "application.json"));
        HashMap<Application, Config> configs = JSONObject.parseObject(configStr, new TypeReference<HashMap<Application, Config>>() {
        });
        Config config = configs.get(application);
        config.setApplication(application);
        config.init();
        return config;
    }

    public static void main(String[] args) {
        ProcessCleaner.cleanWebDriver();
        LOGGER.info("Start crawling contents.");
        long totalStart = System.currentTimeMillis();
        String appName = args.length > 0 ? args[0] : null;
        Application application = Application.determineApplication(appName);

        LOGGER.info("Running application: {}", application.name());
        Config config = loadApplicationConfig(application);
        try {
            application.executor.execute(config);
        } catch (Exception e) {
            LOGGER.error("Unknown error found: ", e);
        }
        LOGGER.info("Total execute time: {}", Tools.formatCostTime(totalStart));
        ProcessCleaner.cleanWebDriver();
        System.exit(0);
    }
}
