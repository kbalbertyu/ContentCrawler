package cn.btimes.service;

import cn.btimes.model.baidu.BaiduAMPResult;
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

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/6/10 9:13
 */
public class BaiduSiteMapUploader implements ServiceExecutorInterface {
    private final Logger logger = LoggerFactory.getLogger(TagGenerator.class);
    @Inject private ApiRequest apiRequest;
    @Inject private WebDriverLauncher webDriverLauncher;
    @Inject private DBManager dbManager;
    private List<SmartAppConfig> smartApps;
    private static final String SITE_MAP_FILE_NAME = "baidu-smart-app-sitemap-%s.txt";
    private static final String SITE_MAP_URL = "https://smartprogram.baidu.com/developer/home/promotion.html?appId=%d&tabCur=search&searchCur=newminiapp";
    private static final String BAIDU_AMP_UPLOAD_URL = "http://data.zz.baidu.com/urls?site=%s&token=%s&type=amp";

    private List<SmartAppConfig> loadSmartApps() {
        String configStr = Tools.readFileToString(FileUtils.getFile(Directory.Customize.path(), "baiduSmartApps.json"));
        return JSONObject.parseArray(configStr, SmartAppConfig.class);
    }

    public void execute(Config config) {
        this.smartApps = this.loadSmartApps();
        this.executeAMP(config);
        this.executeSmartApp(config);
    }

    private void executeAMP(Config config) {
        String fetchUrl = String.format("/article/fetchBaiduAMPSiteMap?only_tag=1&article_days=%d", config.getBaiduAMPDaysBefore());
        List<String> urls = this.fetchSiteMapUrls(fetchUrl, config);
        if (urls == null || urls.size() == 0) {
            logger.warn("No AMP site map urls found");
            return;
        }
        String postUrl = String.format(BAIDU_AMP_UPLOAD_URL, config.getBaiduAMPSite(), config.getBaiduAMPToken());
        urls = urls.subList(0, 1000);
        String data = StringUtils.join(urls, StringUtils.LF);

        String body = this.postBaidAMPSiteMap(postUrl, data);
        BaiduAMPResult result = JSONObject.parseObject(body, BaiduAMPResult.class);
        if (result.getError() != 0) {
            logger.error("SiteMap urls not imported: {}", result.getMessage());
            return;
        }
        logger.info("SiteMap urls imported: success={}, remain={}, not same site={}, invalid={}",
            result.getSuccessAmp(), result.getReaminAmp(), result.getNotSameSite(), result.getNotValid());
        urls.forEach(url -> dbManager.save(new ActionLog(url), ActionLog.class));
    }

    /**
     * <a href="https://liqita.iteye.com/blog/2221082">Reference</a>
     */
    private String postBaidAMPSiteMap(String postUrl, String data) {
        StringBuilder result = new StringBuilder();
        PrintWriter out = null;
        BufferedReader in = null;
        try {
            //建立URL之间的连接
            URLConnection conn = new URL(postUrl).openConnection();
            //设置通用的请求属性
            conn.setRequestProperty("Host","data.zz.baidu.com");
            conn.setRequestProperty("User-Agent", "curl/7.12.1");
            conn.setRequestProperty("Content-Length", String.valueOf(data.length()));
            conn.setRequestProperty("Content-Type", "text/plain");

            //发送POST请求必须设置如下两行
            conn.setDoInput(true);
            conn.setDoOutput(true);

            //获取conn对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            //发送请求参数
            out.print(data);
            //进行输出流的缓冲
            out.flush();
            //通过BufferedReader输入流来读取Url的响应
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (Exception e) {
            logger.error("Unable to send out the SiteMap urls.", e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                logger.error("Unable to close stream.");
            }
        }
        return result.toString();
    }

    private void executeSmartApp(Config config) {
        for (SmartAppConfig smartAppConfig : smartApps) {
            WebDriver driver = null;
            try {
                SmartApp app = smartAppConfig.getApp();
                String fetchUrl = String.format("/article/fetchBaiduSmartAppSiteMap?tag_days=%d&article_days=%d&category=%d",
                    smartAppConfig.getDaysBeforeTag(), smartAppConfig.getDaysBefore(), smartAppConfig.getCategory().id);

                List<String> urls = this.fetchSiteMapUrls(fetchUrl, config);
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
                    logger.warn("No more remaining items for submitting: to upload={}, try upload to weekly submission.", urls.size());
                    this.upload(smartAppConfig, driver, urls, By.name("batch"));
                    continue;
                }

                urls = urls.size() <= itemsRemains ? urls : urls.subList(0, itemsRemains);
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

    /**
     * Fetch SiteMap urls for Baidu
     */
    private List<String> fetchSiteMapUrls(String fetchUrl, Config config) {
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
