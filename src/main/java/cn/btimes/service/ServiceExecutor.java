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
import com.amzass.utils.common.Constants;
import com.amzass.utils.common.ProcessCleaner;
import com.amzass.utils.common.Tools;
import com.google.inject.Inject;
import com.kber.commons.DBManager;
import com.mailman.model.common.WebApiResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 12/24/2018 8:33 AM
 */
public class ServiceExecutor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject private WebDriverLauncher webDriverLauncher;
    @Inject private Messengers messengers;
    @Inject private EmailSenderHelper emailSenderHelper;
    @Inject private ApiRequest apiRequest;
    @Inject private DBManager dbManager;

    protected String[] allowedSources() {
        String text = StringUtils.trim(Tools.getCustomizingValue("ALLOWED_SOURCES"));
        if (StringUtils.isBlank(text)) {
            return null;
        }
        return StringUtils.split(text, ",");
    }

    protected String[] jsDisabledSources() {
        return new String[] {"CNBeta", "YiCai", "JieMian", "COM163", "WallStreetCN", "NBD", "IYiOu",
            "IFeng", "EntGroup", "CSCOMCN", "LUXE", "LvJie", "PinChain", "LadyMax", "CTO51"};
    }

    protected List<Source> getSources() {
        List<Source> sources = new ArrayList<>();
        sources.add(ApplicationContext.getBean(Sina.class));
        sources.add(ApplicationContext.getBean(SinaFinance.class));
        sources.add(ApplicationContext.getBean(ThePaper.class));
        sources.add(ApplicationContext.getBean(QQ.class));
        sources.add(ApplicationContext.getBean(IFengTravel.class));

        // JS Disabled source must be after the others
        sources.add(ApplicationContext.getBean(JieMian.class));
        sources.add(ApplicationContext.getBean(COM163.class));
        sources.add(ApplicationContext.getBean(WallStreetCN.class));
        sources.add(ApplicationContext.getBean(CNBeta.class));
        sources.add(ApplicationContext.getBean(YiCai.class));
        sources.add(ApplicationContext.getBean(NBD.class));
        sources.add(ApplicationContext.getBean(IYiOu.class));
        sources.add(ApplicationContext.getBean(IFeng.class));
        sources.add(ApplicationContext.getBean(EntGroup.class));
        sources.add(ApplicationContext.getBean(CSCOMCN.class));
        sources.add(ApplicationContext.getBean(LUXE.class));
        sources.add(ApplicationContext.getBean(LvJie.class));
        sources.add(ApplicationContext.getBean(PinChain.class));
        sources.add(ApplicationContext.getBean(LadyMax.class));
        sources.add(ApplicationContext.getBean(CTO51.class));
        return sources;
    }

    public void execute(Config config) {
        messengers.clear();
        this.statistic();
        this.deleteOldArticleLogs();
        this.deleteDownloadedFiles();
        this.syncSavedArticles();
        WebDriver driver = webDriverLauncher.start(config, false);
        boolean jsDisabled = false;
        for (Source source : this.getSources()) {
            String sourceName = StringUtils.substringAfterLast(source.getClass().getName(), ".");
            String[] allowedSources = this.allowedSources();
            if (ArrayUtils.isNotEmpty(allowedSources) && !ArrayUtils.contains(allowedSources, sourceName)) {
                continue;
            }

            String[] jsDisabledSources = this.jsDisabledSources();
            boolean disableJS = ArrayUtils.isNotEmpty(jsDisabledSources) && ArrayUtils.contains(jsDisabledSources, sourceName);
            if (disableJS && !jsDisabled) {
                ProcessCleaner.cleanWebDriver();
                WebDriverLauncher.adminCookies.clear();
                driver = webDriverLauncher.start(config, true);
                jsDisabled = true;
            }
            logger.info("Start fetching from source: {}", sourceName);
            for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
                try {
                    source.execute(driver, config);
                } catch (TimeoutException e) {
                    logger.error("Connection timeout, restart WebDriver and retry fetching: {}", sourceName);
                    ProcessCleaner.cleanWebDriver();
                    driver = webDriverLauncher.start(config, disableJS);
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
            this.sendErrorMessage();
        }
    }

    private void sendErrorMessage() {
        Date date = new Date();
        String hour = DateFormatUtils.format(date, "HH");
        if (!StringUtils.equals(hour, "12")) {
            return;
        }
        String logId = "Send_Message_" + DateFormatUtils.format(date, "yyyy-MM-dd");
        ActionLog log = dbManager.readById(logId, ActionLog.class);
        if (log != null) {
            return;
        }
        this.sendMessage(this.messengers);
        dbManager.save(new ActionLog(logId), ActionLog.class);
    }

    protected void statistic() {
        Date date = new Date();
        String hour = DateFormatUtils.format(date, "E", Country.US.locale());
        if (!StringUtils.equalsIgnoreCase(hour, "Fri")) {
            return;
        }
        String logId = "Send_Message_" + DateFormatUtils.format(date, "yyyy-MM-dd", Country.US.locale());
        ActionLog log = dbManager.readById(logId, ActionLog.class);
        if (log != null) {
            return;
        }
        ApplicationContext.getBean(Statistics.class).execute();
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
    protected void syncSavedArticles() {
        WebApiResult result = apiRequest.get("/article/fetchCrawledLinks");
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

    private void sendMessage(Messengers messengers) {
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
        this.emailSenderHelper.send("ContentCrawler Error Messages", sb.toString(), "tansoyu@gmail.com");
    }
}
