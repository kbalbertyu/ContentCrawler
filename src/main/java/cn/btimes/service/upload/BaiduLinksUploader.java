package cn.btimes.service.upload;

import cn.btimes.model.baidu.BaiduLink;
import cn.btimes.model.baidu.BaiduResult;
import cn.btimes.model.baidu.SmartApp;
import cn.btimes.model.baidu.SmartAppConfig;
import cn.btimes.model.common.Config;
import cn.btimes.service.WebDriverLauncher;
import cn.btimes.utils.Common;
import com.alibaba.fastjson.JSONObject;
import com.amzass.enums.common.Directory;
import com.amzass.model.common.ActionLog;
import com.amzass.service.common.ApplicationContext;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.ui.utils.UITools;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.PageUtils;
import com.amzass.utils.common.RegexUtils;
import com.amzass.utils.common.RegexUtils.Regex;
import com.amzass.utils.common.Tools;
import com.google.inject.Inject;
import com.kber.commons.DBManager;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/6/10 9:13
 */
public class BaiduLinksUploader extends AbstractLinksUploader {
    private final Logger logger = LoggerFactory.getLogger(BaiduLinksUploader.class);
    @Inject private WebDriverLauncher webDriverLauncher;
    @Inject private DBManager dbManager;
    private List<SmartAppConfig> smartApps;
    private static final String SITE_MAP_FILE_NAME = "baidu-smart-app-sitemap-%s.txt";
    private static final String SITE_MAP_URL = "https://smartprogram.baidu.com/developer/home/promotion.html?appId=%d&tabCur=search&searchCur=newminiapp";

    private List<SmartAppConfig> loadSmartApps() {
        String configStr = Tools.readFileToString(FileUtils.getFile(Directory.Customize.path(), "baiduSmartApps.json"));
        return JSONObject.parseArray(configStr, SmartAppConfig.class);
    }

    public void execute(Config config) {
        this.smartApps = this.loadSmartApps();
        this.executeLinksUpload(config);
        this.executeSmartApp(config);
    }

    private void executeLinksUpload(Config config) {
        for (BaiduLink baiduLink : BaiduLink.values()) {
            logger.info("Uploading Baidu links of {}", baiduLink.name());
            String fetchUrl = String.format("/article/fetchBaiduSiteMap?dev=%s&only_original=&article_days=%d",
                baiduLink.type, config.getBaiduDaysBefore());
            List<String> urls = this.fetchLinks(fetchUrl, config, null);
            if (urls == null || urls.size() == 0) {
                logger.warn("No site map urls found");
                continue;
            }
            this.uploadSiteMap(urls, config, baiduLink);
        }
    }

    private void uploadSiteMap(List<String> urls, Config config, BaiduLink baiduLink) {
        String postUrl = baiduLink.postUrl(config.getBaiduSite(), config.getBaiduToken());
        int itemsPerUpload = 1000;
        int size = urls.size();
        for (int i = 0; ; i++) {
            List<String> urlsForUpload = this.urlsSegment(urls, size, i, itemsPerUpload);
            if (CollectionUtils.isEmpty(urlsForUpload)) {
                logger.warn("No urls found for Shenma submission");
                break;
            }
            String data = StringUtils.join(urlsForUpload, StringUtils.LF);

            String body = this.postLinks(postUrl, data, BaiduLink.API_HOST);
            BaiduResult result = JSONObject.parseObject(body, BaiduResult.class);
            if (result.getError() != 0) {
                logger.error("SiteMap urls not imported: {}", result.getMessage());
                return;
            }
            logger.info("SiteMap urls imported: {}", result);
            urlsForUpload.forEach(url -> dbManager.save(new ActionLog(url), ActionLog.class));
            if (!result.canUpload(baiduLink)) {
                break;
            }
        }
    }

    private void executeSmartApp(Config config) {
        for (SmartAppConfig smartAppConfig : smartApps) {
            WebDriver driver = null;
            try {
                SmartApp app = smartAppConfig.getApp();
                String fetchUrl = String.format("/article/fetchBaiduSmartAppSiteMap?tag_days=%d&article_days=%d&category=%d",
                    smartAppConfig.getDaysBeforeTag(), smartAppConfig.getDaysBefore(), smartAppConfig.getCategory().id);

                List<String> urls = this.fetchLinks(fetchUrl, config, null);
                if (urls == null || urls.size() == 0) {
                    logger.warn("No site map urls found: {}", app.name());
                    continue;
                }

                logger.info("Uploading site map file of {}, urls = {}", app.name(), urls.size());
                driver = webDriverLauncher.startWithoutLogin(app.name());

                driver.get(String.format(SITE_MAP_URL, app.id));
                WaitTime.Normal.execute();

                List<WebElement> buttons = driver.findElements(By.className("upload-btn"));
                boolean hasPopup = false;
                for (WebElement button : buttons) {
                    if (StringUtils.contains(button.getText(), "上传文件")) {
                        PageUtils.click(driver, button);
                        hasPopup = true;
                        break;
                    }
                }
                if (!hasPopup) {
                    logger.error("Upload popup not clicked.");
                    if (!UITools.confirmed("Please click the upload tab manually.")) {
                        continue;
                    }
                }

                WaitTime.Short.execute();
                Document doc = Jsoup.parse(driver.getPageSource());
                int itemsRemains = this.getItemsRemains(doc);
                if (itemsRemains == 0) {
                    logger.warn("No more remaining items for submitting: to upload={}, try upload to weekly submission.", urls.size());
                    this.upload(smartAppConfig, driver, urls, By.name("batch"));
                    continue;
                }

                urls = urls.size() <= itemsRemains ? urls : urls.subList(0, itemsRemains);
                logger.info("Submitting daily urls of SmarApp: {}", urls.size());
                this.upload(smartAppConfig, driver, urls, By.name("realtime"));
            } finally {
                if (driver != null) {
                    driver.close();
                    driver.quit();
                }
            }
        }
    }

    private void upload(SmartAppConfig smartAppConfig, WebDriver driver, List<String> urls, By inputBy) {
        File siteMapFile = FileUtils.getFile(Directory.Tmp.path(), String.format(SITE_MAP_FILE_NAME, smartAppConfig.getApp()));
        Tools.writeLinesToFile(siteMapFile, urls);

        driver.findElement(inputBy).sendKeys(siteMapFile.getAbsolutePath());
        WaitTime.Normal.execute();

        urls.forEach(url -> dbManager.save(new ActionLog(url), ActionLog.class));
    }

    private int getItemsRemains(Document doc) {
        String text = HtmlParser.text(doc, ".upload-item-right:contains(天级别sitemap资源收录) .item-tips");
        String numberText = RegexUtils.getMatched(text, "剩余可上传(\\d+)");
        numberText = numberText.replaceAll(Regex.NON_DIGITS.val(), StringUtils.EMPTY);
        return NumberUtils.toInt(numberText);
    }

    public static void main(String[] args) {
        String appName = args.length > 0 ? args[0] : null;
        Config config = Common.loadApplicationConfig(appName);
        ApplicationContext.getBean(BaiduLinksUploader.class).execute(config);
        System.exit(0);
    }
}
