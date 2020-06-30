package cn.btimes.model.common;

import cn.btimes.service.*;
import com.amzass.service.common.ApplicationContext;
import com.fortis.service.AmazonCrawler;
import org.apache.commons.lang3.StringUtils;
import cn.btimes.service.upload.BaiduLinksUploader;
import cn.btimes.service.upload.ShenmaLinksUploader;
import cn.btimes.service.upload.SogouLinksUploader;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-07-12 10:36 AM
 */
public enum Application {
    BTimes(ApplicationContext.getBean(ServiceExecutor.class)),
    BTimesJP(ApplicationContext.getBean(jp.btimes.service.ServiceExecutor.class)),
    Kpopstarz(ApplicationContext.getBean(cn.kpopstarz.service.ServiceExecutor.class)),
    BaiduLinksUploader(ApplicationContext.getBean(BaiduLinksUploader.class)),
    ShenmaLinksUploader(ApplicationContext.getBean(ShenmaLinksUploader.class)),
    SogouLinksUploader(ApplicationContext.getBean(SogouLinksUploader.class)),
    TagGenerator(ApplicationContext.getBean(cn.btimes.service.TagGenerator.class)),
    YiQiZeng(ApplicationContext.getBean(YiQiZengCrawler.class)),
    RelatedArticle(ApplicationContext.getBean(RelatedArticleHandler.class)),
    AmazonCrawler(ApplicationContext.getBean(AmazonCrawler.class)),
    RelatedArticleHandler(ApplicationContext.getBean(RelatedArticleHandler.class)),
    DBBackUpHandler(ApplicationContext.getBean(DBBackUpHandler.class)),
    NewsFlowCrawler(ApplicationContext.getBean(NewsFlowCrawler.class));

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

    public static List<Application> determineApplications(String[] args) {
        List<Application> applications = new ArrayList<>();
        if (args == null || args.length == 0) {
            Application application = determineApplication(null);
            applications.add(application);
        } else {
            for (String appName : args) {
                Application application = determineApplication(appName);
                applications.add(application);
            }
        }
        return applications;
    }
}
