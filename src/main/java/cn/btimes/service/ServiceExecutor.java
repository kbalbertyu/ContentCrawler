package cn.btimes.service;

import cn.btimes.model.common.Config;
import cn.btimes.model.common.Messenger;
import cn.btimes.model.common.Messengers;
import cn.btimes.source.*;
import cn.btimes.utils.Common;
import cn.btimes.utils.PageUtils;
import com.alibaba.fastjson.JSONObject;
import com.amzass.enums.common.Country;
import com.amzass.model.common.ActionLog;
import com.amzass.service.common.ApplicationContext;
import com.amzass.ui.utils.UITools;
import com.amzass.utils.PageLoadHelper;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Constants;
import com.amzass.utils.common.Tools;
import com.google.inject.Inject;
import com.kber.commons.DBManager;
import com.mailman.model.common.WebApiResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 12/24/2018 8:33 AM
 */
public class ServiceExecutor implements ServiceExecutorInterface {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject private WebDriverLauncher webDriverLauncher;
    @Inject private Messengers messengers;
    @Inject private EmailSenderHelper emailSenderHelper;
    @Inject private ApiRequest apiRequest;
    @Inject private DBManager dbManager;
    @Inject private TagGenerator tagGenerator;
    private static final boolean NO_DELETE_TMP_FILES = StringUtils.isNotBlank(Tools.getCustomizingValue("NO_DELETE_TMP_FILES"));
    private static final boolean TEST_MODE = StringUtils.isNotBlank(Tools.getCustomizingValue("TEST_MODE"));
    private static final boolean PAUSE_DRIVER = StringUtils.isNotBlank(Tools.getCustomizingValue("PAUSE_DRIVER"));
    private static final String FETCH_CRAWLED_LINKS_PAST_HOURS = Tools.getCustomizingValue("FETCH_CRAWLED_LINKS_PAST_HOURS");

    protected String[] allowedSources() {
        String text = StringUtils.trim(Tools.getCustomizingValue("ALLOWED_SOURCES"));
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return StringUtils.split(text, ",");
    }

    protected String[] excludedSources() {
        String text = StringUtils.trim(Tools.getCustomizingValue("EXCLUDE_SOURCES"));
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return StringUtils.split(text, ",");
    }

    protected List<Source> getSources() {
        List<Source> sources = this.getBTCNSources();
        sources.add(ApplicationContext.getBean(ChinaNetPhoto.class));
        sources.add(0, ApplicationContext.getBean(BTimes.class));
        return sources;
    }

    protected List<Source> getBTCNSources() {
        List<Source> sources = new ArrayList<>();
        sources.add(ApplicationContext.getBean(HaiWaiNet.class));
        sources.add(ApplicationContext.getBean(ZhiTongCaiJing.class));
        sources.add(ApplicationContext.getBean(CBR21.class));
        sources.add(ApplicationContext.getBean(ZQRB.class));
        sources.add(ApplicationContext.getBean(TechWeb.class));
        sources.add(ApplicationContext.getBean(ZNFinNews.class));
        sources.add(ApplicationContext.getBean(CYZone.class));
        sources.add(ApplicationContext.getBean(ChinaNews.class));
        sources.add(ApplicationContext.getBean(NewsCN.class));
        sources.add(ApplicationContext.getBean(People.class));
        sources.add(ApplicationContext.getBean(PRNAsia.class));
        sources.add(ApplicationContext.getBean(CSCOMCN.class));
        sources.add(ApplicationContext.getBean(CECN.class));
        sources.add(ApplicationContext.getBean(NewsCCTV.class));
        return sources;
    }

    protected List<Source> getBTCNUsedSources() {
        List<Source> sources = new ArrayList<>();
        sources.add(ApplicationContext.getBean(Sina.class));
        sources.add(ApplicationContext.getBean(SinaFinance.class));
        sources.add(ApplicationContext.getBean(ThePaper.class));
        sources.add(ApplicationContext.getBean(IFengTravel.class));
        sources.add(ApplicationContext.getBean(JieMian.class));
        sources.add(ApplicationContext.getBean(COM163.class));
        sources.add(ApplicationContext.getBean(CNBeta.class));
        sources.add(ApplicationContext.getBean(YiCai.class));
        sources.add(ApplicationContext.getBean(NBD.class));
        sources.add(ApplicationContext.getBean(IFeng.class));
        sources.add(ApplicationContext.getBean(EntGroup.class));
        sources.add(ApplicationContext.getBean(LUXE.class));
        sources.add(ApplicationContext.getBean(LvJie.class));
        sources.add(ApplicationContext.getBean(PinChain.class));
        sources.add(ApplicationContext.getBean(LadyMax.class));
        sources.add(ApplicationContext.getBean(ZJOLbiz.class));
        sources.add(ApplicationContext.getBean(ZJOLec.class));
        sources.add(ApplicationContext.getBean(KR36.class));
        sources.add(ApplicationContext.getBean(HuanQiu.class));
        sources.add(ApplicationContext.getBean(STDaily.class));
        sources.add(ApplicationContext.getBean(TechNode.class));
        sources.add(ApplicationContext.getBean(STCN.class));
        sources.add(ApplicationContext.getBean(CNStock.class));
        sources.add(ApplicationContext.getBean(EastMoney.class));
        sources.add(ApplicationContext.getBean(CaiFuHao.class));
        sources.add(ApplicationContext.getBean(BJNews.class));
        sources.add(ApplicationContext.getBean(ChangJiangTimes.class));
        return sources;
    }

    private void preExecute(Config config) {
        this.deleteTmpFiles();
        // this.statistic(config);
        this.deleteOldArticleLogs();
        this.deleteDownloadedFiles();
        this.syncSavedArticles(config);
    }

    public void execute(Config config) {
        messengers.clear();
        this.preExecute(config);
        WebDriver driver = webDriverLauncher.start(config);
        if (TEST_MODE) {
            this.enterTestMode(driver, config);
        }
        if (PAUSE_DRIVER) {
            UITools.confirm("Paused");
        }
        for (Source source : this.getSources()) {
            String sourceName = StringUtils.substringAfterLast(source.getClass().getName(), ".");
            if (!this.allowed(sourceName)) {
                logger.warn("Source is skipped: {}", sourceName);
                continue;
            }
            logger.info("Start fetching from source: {}", sourceName);
            for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
                try {
                    source.execute(driver, config);
                } catch (WebDriverException e) {
                    logger.error("Connection timeout, restart WebDriver and retry fetching: {}", sourceName, e);
                    webDriverLauncher.close(driver);
                    driver = webDriverLauncher.start(config);
                    continue;
                } catch (Exception e) {
                    String message = String.format("Error found in executing: %s", this.getClass());
                    logger.error(message, e);
                    Messenger messenger = new Messenger(sourceName, message);
                    this.messengers.add(messenger);
                    PageUtils.savePage4ErrorHandling(driver, String.valueOf(System.currentTimeMillis()), "execute");
                }
                logger.info("Source fetching finished: {}", sourceName);
                break;
            }
        }
        if (this.messengers.isNotEmpty()) {
            // this.sendErrorMessage(config);
        }
    }

    private boolean allowed(String sourceName) {
        String[] allowedSources = this.allowedSources();
        if (ArrayUtils.isNotEmpty(allowedSources) && !ArrayUtils.contains(allowedSources, sourceName)) {
            return false;
        }


        String[] excludedSources = this.excludedSources();
        if (ArrayUtils.isNotEmpty(excludedSources) && ArrayUtils.contains(excludedSources, sourceName)) {
            return false;
        }

        return true;
    }

    private void flushArticleCaches(WebDriver driver, Config config) {
        String ids = "521941,521940,521939,521938,521936,521937,521935,521934,521931,521932,521990,521989,521982,521970,521969,521965,521959,521958,521957,521956,521955,521953,521952,521949,521947,521946,521945,521944,521943,521942";
        String[] idArr = StringUtils.split(ids, ",");
        for (String id : idArr) {
            String url = config.getAdminUrl() + "/pages/publish/publish/form.php?w=u&ar_id=" + id;
            driver.get(url);
            WaitTime.Shortest.execute();
            PageUtils.click(driver, By.cssSelector("button.btn-info.article_submit"));
            PageLoadHelper.present(driver, By.id("flist"), WaitTime.Normal);
            logger.info("Updated: " + id);
        }
    }

    private void enterTestMode(WebDriver driver, Config config) {
        driver.get(config.getFrontUrl() + "/mode.php");
        PageUtils.click(driver, By.id("enable"));
        PageUtils.click(driver, By.cssSelector("input[type=submit]"));
    }

    private void deleteTmpFiles() {
        if (NO_DELETE_TMP_FILES) {
            return;
        }
        String logId = String.format("Delete_Tmp_Files_%s", DateFormatUtils.format(new Date(), "yyyyMMdd"));
        ActionLog log = dbManager.readById(logId, ActionLog.class);
        if (log != null) {
            return;
        }
        File path = FileUtils.getFile(SystemUtils.getUserHome(), "AppData", "Local", "Temp");
        for (File file : path.listFiles()) {
            FileUtils.deleteQuietly(file);
        }
        dbManager.save(new ActionLog(logId), ActionLog.class);
        logger.info("Temp directory is cleaned: {}", path.getAbsolutePath());
    }

    private void updateArticles(Config config, WebDriver driver) {
        List<String> aids = Tools.readFile(FileUtils.getFile("C:\\Work\\Projects\\BT\\uv_article.csv"));
        for (String aid : aids) {
            String url = String.format("%s/pages/publish/publish/form.php?w=u&skip_field_validation=1&ar_id=%s", config.getAdminUrl(), aid);
            driver.get(url);
            WaitTime.Normal.execute();
            List<WebElement> elements = driver.findElements(By.className("article_submit"));
            for (WebElement element : elements) {
                String text = StringUtils.trim(element.getText());
                if (!StringUtils.equals(text, "保存")) {
                    continue;
                }
                PageUtils.click(driver, element);
                break;
            }
            WaitTime.Normal.execute();
        }
    }

    private void generateArticleTags(Config config) {
        String logId = "TagGenerate" + DateFormatUtils.format(new Date(), "yyyyMMddHH");
        ActionLog log = dbManager.readById(logId, ActionLog.class);
        if (log != null) {
            return;
        }
        tagGenerator.execute(config);
        dbManager.save(new ActionLog(logId), ActionLog.class);
    }

    private void sendErrorMessage(Config config) {
        Date date = new Date();
        String hour = DateFormatUtils.format(date, "HH");
        if (!StringUtils.equals(hour, "12")) {
            return;
        }
        String logId = String.format("Send_Error_Message_%s_%s", config.getApplication(),
            DateFormatUtils.format(date, "yyyy-MM-dd"));
        ActionLog log = dbManager.readById(logId, ActionLog.class);
        if (log != null) {
            return;
        }
        this.sendMessage(this.messengers, config);
        dbManager.save(new ActionLog(logId), ActionLog.class);
    }

    private void statistic(Config config) {
        Date date = new Date();
        String hour = DateFormatUtils.format(date, "E", Country.US.locale());
        if (!StringUtils.equalsIgnoreCase(hour, "Fri")) {
            return;
        }
        String logId = String.format("Send_Stat_Message_%s_%s", config.getApplication(),
            DateFormatUtils.format(date, "yyyy-MM-dd", Country.US.locale()));
        ActionLog log = dbManager.readById(logId, ActionLog.class);
        if (log != null) {
            return;
        }
        ApplicationContext.getBean(Statistics.class).execute(config);
        dbManager.save(new ActionLog(logId), ActionLog.class);
    }

    private void deleteOldArticleLogs() {
        Date date = new Date();

        String logId = "Delete_Logs_" + DateFormatUtils.format(date, "yyyy-MM", Country.US.locale());
        ActionLog log = dbManager.readById(logId, ActionLog.class);
        if (log != null) {
            return;
        }
        date = DateUtils.addDays(date, -30);
        String dateString = DateFormatUtils.format(date, "yyyy-MM-dd HH:mm:ss");
        String sql = String.format("DELETE FROM action_log WHERE lasttime < '%s'", dateString);
        dbManager.execute(sql);
        dbManager.save(new ActionLog(logId), ActionLog.class);
    }

    /**
     * Read article original links from website via API,
     * save to DB in action_log table if not exists.
     */
    private void syncSavedArticles(Config config) {
        WebApiResult result = apiRequest.get("/article/fetchCrawledLinks?hours=" + FETCH_CRAWLED_LINKS_PAST_HOURS, config);
        if (StringUtils.equals(result.getCode(), "0")) {
            logger.error("Unable to fetch articles: {}", result.getMessage());
            return;
        }
        List<String> links = JSONObject.parseArray(result.getData(), String.class);
        if (links.size() == 0) {
            logger.warn("No articles fetched.");
            return;
        }
        for (String link : links) {
            String logId = Common.toMD5(link);
            ActionLog log = dbManager.readById(logId, ActionLog.class);
            if (log != null) {
                continue;
            }
            dbManager.save(new ActionLog(logId), ActionLog.class);
        }
    }

    /**
     * Delete downloaded files once every Monday
     */
    private void deleteDownloadedFiles() {
        Date date = new Date();
        String hour = DateFormatUtils.format(date, "E", Country.US.locale());
        if (!StringUtils.equalsIgnoreCase(hour, "Mon")) {
            return;
        }
        String logId = "Delete_Downloaded_" + DateFormatUtils.format(date, "yyyy-MM-dd", Country.US.locale());
        ActionLog log = dbManager.readById(logId, ActionLog.class);
        if (log != null) {
            return;
        }

        String[] rootPaths = new String[] {WebDriverLauncher.DOWNLOAD_PATH, System.getProperty("user.dir") + "/html"};
        for (String rootPath : rootPaths) {
            File root = FileUtils.getFile(rootPath);
            if (!root.isDirectory()) {
                logger.error("Download directory not exists: {}", rootPath);
                return;
            }
            int i = 0, j = 0;
            for (File file : root.listFiles()) {
                if (FileUtils.deleteQuietly(file)) {
                    i++;
                    continue;
                }
                j++;
            }
            logger.info("Downloaded files deleted: success={}, fail={}", i, j);
        }
        dbManager.save(new ActionLog(logId), ActionLog.class);
    }

    private void sendMessage(Messengers messengers, Config config) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table>");
        for (Messenger messenger : messengers.getList()) {
            sb.append("<tr>");
            sb.append("<td>");
            sb.append(messenger.getSource());
            sb.append("</td>");
            sb.append("<td>");
            sb.append(messenger.getMessage());
            sb.append("</td>");
            sb.append("</tr>");
        }
        sb.append("</table>");
        String subject = String.format("[%s]ContentCrawler Error Messages", config.getApplication());
        this.emailSenderHelper.send(subject, sb.toString(), "tansoyu@gmail.com");
    }
}
