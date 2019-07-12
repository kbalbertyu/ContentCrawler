package cn.btimes.model.common;

import cn.btimes.service.*;
import com.amzass.service.common.ApplicationContext;
import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-07-12 10:36 AM
 */
public enum Application {
    BTimes(ApplicationContext.getBean(ServiceExecutor.class)),
    Kpopstarz(ApplicationContext.getBean(cn.kpopstarz.service.ServiceExecutor.class)),
    BaiduSmartAppSiteMap(ApplicationContext.getBean(BaiduSiteMapUploader.class)),
    TagGenerator(ApplicationContext.getBean(cn.btimes.service.TagGenerator.class)),
    YiQiZeng(ApplicationContext.getBean(YiQiZengCrawler.class)),
    RelatedArticle(ApplicationContext.getBean(RelatedArticleHandler.class));

    Application(ServiceExecutorInterface executor) {
        this.executor = executor;
    }

    public static Application determineApplication(String name) {
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

    public ServiceExecutorInterface executor;
}
