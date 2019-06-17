package cn.btimes.service;

import cn.btimes.model.baidu.SmartApp;
import cn.btimes.model.baidu.SmartAppConfig;
import cn.btimes.model.common.Config;
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
import com.mailman.model.common.WebApiResult;
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
public class BaiduSiteMapUploader implements ServiceExecutorInterface {
    private final Logger logger = LoggerFactory.getLogger(TagGenerator.class);
    @Inject private ApiRequest apiRequest;
    @Inject private WebDriverLauncher webDriverLauncher;
    @Inject private DBManager dbManager;
    private static final List<SmartAppConfig> smartApps = loadSmartApps();
    private static final String SITE_MAP_FILE_NAME = "baidu-smart-app-sitemap-%s.txt";
    private static final String SITE_MAP_URL = "https://smartprogram.baidu.com/developer/home/promotion.html?appId=%d&tabCur=search&searchCur=newminiapp";

    private static List<SmartAppConfig> loadSmartApps() {
        String configStr = Tools.readFileToString(FileUtils.getFile(Directory.Customize.path(), "baiduSmartApps.json"));
        return JSONObject.parseArray(configStr, SmartAppConfig.class);
    }

    public void execute(Config config) {
        for (SmartAppConfig smartAppConfig : smartApps) {
            WebDriver driver = null;
            try {
                SmartApp app = smartAppConfig.getApp();
                List<String> urls = this.generateSiteMapFile(smartAppConfig, config);
                if (urls == null || urls.size() == 0) {
                    logger.warn("No site map urls found: {}", app.name());
                    continue;
                }

                logger.info("Uploading site map file: {}", app.name());
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
                    logger.warn("No more remaining items for submitting: to upload={}", urls.size());
                    continue;
                }

                urls = urls.size() <= itemsRemains ? urls : urls.subList(0, itemsRemains - 1);
                File siteMapFile = FileUtils.getFile(Directory.Tmp.path(), String.format(SITE_MAP_FILE_NAME, smartAppConfig.getApp()));
                Tools.writeLinesToFile(siteMapFile, urls);

                driver.findElement(By.name("realtime")).sendKeys(siteMapFile.getAbsolutePath());
                WaitTime.Normal.execute();

                urls.forEach(url -> dbManager.save(new ActionLog(url), ActionLog.class));
            } finally {
                if (driver != null) {
                    driver.close();
                    driver.quit();
                }
            }
        }
    }

    private int getItemsRemains(Document doc) {
        String text = HtmlParser.text(doc, ".upload-item-right:contains(天级别sitemap资源收录) .item-tips");
        String numberText = RegexUtils.getMatched(text, "剩余可上传(\\d+)");
        numberText = numberText.replaceAll(Regex.NON_DIGITS.val(), StringUtils.EMPTY);
        return NumberUtils.toInt(numberText);
    }

    /**
     * Generate SiteMap for Baidu Smart App
     */
    private List<String> generateSiteMapFile(SmartAppConfig smartAppConfig, Config config) {
        String fetchUrl = String.format("/article/fetchBaiduSmartAppSiteMap?tag_days=%d&article_days=%d&category=%d",
            smartAppConfig.getDaysBeforeTag(), smartAppConfig.getDaysBefore(), smartAppConfig.getCategory().id);
        WebApiResult result = apiRequest.get(fetchUrl, config);
        if (result == null || StringUtils.isBlank(result.getData())) {
            return null;
        }
        List<String> urls = JSONObject.parseArray(result.getData(), String.class);
        if (urls.size() == 0) {
            return null;
        }

        urls.removeIf(url -> dbManager.readById(url, ActionLog.class) != null);
        if (urls.size() == 0) {
            return null;
        }
        return urls;
    }

    public static void main(String[] args) {
        String appName = args.length > 0 ? args[0] : null;
        Config config = Common.loadApplicationConfig(appName);
        ApplicationContext.getBean(BaiduSiteMapUploader.class).execute(config);
        System.exit(0);
    }
}
