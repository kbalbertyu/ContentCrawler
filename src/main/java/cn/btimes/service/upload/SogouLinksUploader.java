package cn.btimes.service.upload;

import cn.btimes.model.common.Config;
import cn.btimes.service.WebDriverLauncher;
import com.amzass.model.common.ActionLog;
import com.amzass.ui.utils.UITools;
import com.amzass.utils.PageLoadHelper;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.amzass.utils.common.PageUtils;
import com.google.inject.Inject;
import com.kber.commons.DBManager;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/8/2 12:38
 */
public class SogouLinksUploader extends AbstractLinksUploader {
    private final Logger logger = LoggerFactory.getLogger(BaiduLinksUploader.class);
    private static final String URL_ID_PREFIX = "Sogou";
    private static final String UPLOAD_URL = "http://zhanzhang.sogou.com/index.php/sitelink/index";
    @Inject private WebDriverLauncher webDriverLauncher;
    @Inject private DBManager dbManager;

    @Override
    public void execute(Config config) {
        WebDriver driver = null;
        try {
            String app = config.getApplication().name();
            driver = webDriverLauncher.startWithoutLogin(app);

            for (SiteType siteType : SiteType.values()) {
                String fetchUrl = String.format("/article/fetchBaiduSiteMap?dev=%s&only_original=&article_days=%d",
                    siteType.name().toLowerCase(), config.getBaiduDaysBefore());

                List<String> urls = this.fetchLinks(fetchUrl, config, URL_ID_PREFIX);
                if (urls == null || urls.size() == 0) {
                    logger.warn("No site map urls found for : {}", siteType.name());
                    continue;
                }

                driver.get(UPLOAD_URL);
                if (!PageLoadHelper.present(driver, By.name("webAdr"), WaitTime.Normal) &&
                    !UITools.confirmed("Please login to Sogou manually.")) {
                    throw new BusinessException("Unable to login to Sogou.");
                }

                this.uploadLinks(driver, siteType, config, urls);
            }
        } finally {
            if (driver != null) {
                driver.close();
                driver.quit();
            }
        }
    }

    private void uploadLinks(WebDriver driver, SiteType siteType, Config config, List<String> urls) {
        int itemsPerUpload = 20;
        int size = urls.size();

        List<String> urlsForUpload = this.urlsSegment(urls, size, 0, itemsPerUpload);
        if (CollectionUtils.isEmpty(urlsForUpload)) {
            logger.warn("No urls found for Sogou submission");
            return;
        }
        String data = StringUtils.join(urlsForUpload, StringUtils.LF);
        PageUtils.setValue(driver, By.name("webAdr"), data);

        List<WebElement> siteTypeElms = driver.findElements(By.name("site_type"));
        for (WebElement siteTypeElm : siteTypeElms) {
            int value = NumberUtils.toInt(siteTypeElm.getAttribute("value"));
            if (value == siteType.type) {
                PageUtils.click(driver, siteTypeElm);
                break;
            }
        }

        PageUtils.setValue(driver, By.name("email"), config.getRecipient());
        driver.findElement(By.id("urlform")).submit();
        WaitTime.Normal.execute();

        String body = driver.getPageSource();
        if (StringUtils.contains(body, "success")) {
            logger.info("Links uploaded successfully");
            urls.forEach(url -> dbManager.save(new ActionLog(urlId(url, URL_ID_PREFIX)), ActionLog.class));
        } else {
            logger.error("Unable to upload the links");
        }
    }

    private enum SiteType {
        PC(1),
        AMP(2);

        private final int type;

        SiteType(int type) {
            this.type = type;
        }
    }
}
