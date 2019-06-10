package cn.btimes.service;

import cn.btimes.model.baidu.SmartAppConfig;
import cn.btimes.model.common.Config;
import cn.btimes.utils.Common;
import com.alibaba.fastjson.JSONObject;
import com.amzass.enums.common.Directory;
import com.amzass.service.common.ApplicationContext;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Constants;
import com.amzass.utils.common.PageUtils;
import com.amzass.utils.common.RegexUtils;
import com.amzass.utils.common.RegexUtils.Regex;
import com.amzass.utils.common.Tools;
import com.google.inject.Inject;
import com.mailman.model.common.WebApiResult;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/6/10 9:13
 */
public class BaiduSiteMapUploader implements ServiceExecutorInterface {
    private final Logger logger = LoggerFactory.getLogger(TagGenerator.class);
    private static final int DAYS_BEFORE = 3;
    @Inject private ApiRequest apiRequest;
    @Inject private WebDriverLauncher webDriverLauncher;
    private static final List<SmartAppConfig> smartApps = loadSmartApps();
    private static final String SITE_MAP_FILE_NAME = "baidu-smart-app-sitemap-%s.txt";
    private static final String SITE_MAP_FILE_NAME_ALL = "baidu-smart-app-sitemap-%s-all.txt";
    private static final String SITE_MAP_URL = "https://smartprogram.baidu.com/developer/home/promotion.html?appId=%d&tabCur=search&searchCur=newminiapp";

    private static List<SmartAppConfig> loadSmartApps() {
        String configStr = Tools.readFileToString(FileUtils.getFile(Directory.Customize.path(), "baiduSmartApps.json"));
        return JSONObject.parseArray(configStr, SmartAppConfig.class);
    }

    public void execute(Config config) {
        for (SmartAppConfig smartAppConfig : smartApps) {
            WebDriver driver = null;
            try {
                File siteMapFile = this.generateSiteMapFile(smartAppConfig, config);
                if (siteMapFile == null || !siteMapFile.exists()) {
                    continue;
                }
                driver = webDriverLauncher.startWithoutLogin(smartAppConfig.getApp().name());

                for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
                    driver.get(String.format(SITE_MAP_URL, smartAppConfig.getApp().id));

                    List<WebElement> buttons = driver.findElements(By.className("upload-btn"));
                    for (WebElement button : buttons) {
                        if (StringUtils.contains(button.getText(), "上传文件")) {
                            PageUtils.click(driver, button);
                            break;
                        }
                    }
                    WaitTime.Short.execute();

                    String absPath = siteMapFile.getAbsolutePath();
                    List<String> lines = Tools.readFile(siteMapFile);

                    Document doc = Jsoup.parse(driver.getPageSource());
                    int itemsRemains = this.getItemsRemains(doc);
                    if (itemsRemains < lines.size()) {
                        logger.warn("No enough remaining items for submitting: to upload={}, remains={}", lines.size(), itemsRemains);
                        break;
                    }

                    PageUtils.setValue(driver, By.name("realtime"), absPath);
                    if (!this.validateUploadStatus(driver, lines.size())) {
                        String backupFilePath = StringUtils.replace(absPath, ".txt", System.currentTimeMillis() + ".txt");
                        try {
                            FileUtils.copyFile(siteMapFile, FileUtils.getFile(backupFilePath));
                        } catch (IOException e) {
                            logger.error("Unable to move failed sitemap file to fail folder: ", e);
                        }
                        logger.error("Failed to upload sitemap file, retry after short waiting.");
                        WaitTime.Normal.execute();
                    } else {
                        logger.info("Success to upload sitemap file.");
                        break;
                    }
                }
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

    private boolean validateUploadStatus(WebDriver driver, int size) {
        String dateText = DateFormatUtils.format(new Date(), "yyyy/MM/dd");
        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            Document doc = Jsoup.parse(driver.getPageSource());
            String rowTest = HtmlParser.text(doc, "table:contains(上传日期) > tbody > tr");
            if (StringUtils.contains(rowTest, String.valueOf(size)) && StringUtils.contains(rowTest, dateText)) {
                return true;
            }
            WaitTime.Short.execute();
        }
        return false;
    }

    /**
     * Generate SiteMap for Baidu Smart App
     */
    private File generateSiteMapFile(SmartAppConfig smartAppConfig, Config config) {
        String fetchUrl = String.format("/article/fetchBaiduSmartAppSiteMap?tag_days=%d&article_days=%d&category=%d",
            DAYS_BEFORE, DAYS_BEFORE, smartAppConfig.getCategory().id);
        WebApiResult result = apiRequest.get(fetchUrl, config);
        if (result == null || StringUtils.isBlank(result.getData())) {
            return null;
        }
        List<String> urls = JSONObject.parseArray(result.getData(), String.class);
        if (urls.size() == 0) {
            return null;
        }

        File fileAll = FileUtils.getFile(Directory.Tmp.path(), String.format(SITE_MAP_FILE_NAME_ALL, smartAppConfig.getApp()));
        if (fileAll.exists()) {
            List<String> uploadedUrls = Tools.readFile(fileAll);
            if (CollectionUtils.isNotEmpty(uploadedUrls)) {
                urls.removeIf(uploadedUrls::contains);
                uploadedUrls.addAll(urls);
            } else {
                uploadedUrls = urls;
            }
            if (urls.size() == 0) {
                return null;
            }
            Tools.writeLinesToFile(fileAll, uploadedUrls);
        } else {
            Tools.writeLinesToFile(fileAll, urls);
        }

        File file = FileUtils.getFile(Directory.Tmp.path(), String.format(SITE_MAP_FILE_NAME, smartAppConfig.getApp()));
        Tools.writeLinesToFile(file, urls);
        return file;
    }

    public static void main(String[] args) {
        String appName = args.length > 0 ? args[0] : null;
        Config config = Common.loadApplicationConfig(appName);
        ApplicationContext.getBean(BaiduSiteMapUploader.class).execute(config);
        System.exit(0);
    }
}
