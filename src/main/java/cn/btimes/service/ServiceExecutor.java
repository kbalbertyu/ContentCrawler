package cn.btimes.service;

import cn.btimes.model.common.Messenger;
import cn.btimes.model.common.Messengers;
import cn.btimes.source.*;
import cn.btimes.utils.Common;
import com.alibaba.fastjson.JSONObject;
import com.amzass.model.common.ActionLog;
import com.amzass.service.common.ApplicationContext;
import com.google.inject.Inject;
import com.kber.commons.DBManager;
import com.mailman.model.common.WebApiResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
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
    private final static List<Source> sources = new ArrayList<>();
    @Inject private WebDriverLauncher webDriverLauncher;
    @Inject private Messengers messengers;
    @Inject private EmailSenderHelper emailSenderHelper;
    @Inject private ApiRequest apiRequest;
    @Inject private DBManager dbManager;

    static {
        sources.add(ApplicationContext.getBean(Sina.class));
        sources.add(ApplicationContext.getBean(ThePaper.class));
        sources.add(ApplicationContext.getBean(YiCai.class));
        sources.add(ApplicationContext.getBean(NBD.class));
        sources.add(ApplicationContext.getBean(COM163.class));
        sources.add(ApplicationContext.getBean(HeXun.class));
        sources.add(ApplicationContext.getBean(LvJie.class));
        sources.add(ApplicationContext.getBean(PinChain.class));
        sources.add(ApplicationContext.getBean(GasGoo.class));
        sources.add(ApplicationContext.getBean(EntGroup.class));
        sources.add(ApplicationContext.getBean(CCDY.class));
        sources.add(ApplicationContext.getBean(JieMian.class));
        sources.add(ApplicationContext.getBean(QQ.class));
        sources.add(ApplicationContext.getBean(LadyMax.class));
        sources.add(ApplicationContext.getBean(LUXE.class));
        sources.add(ApplicationContext.getBean(IYiOu.class));
        sources.add(ApplicationContext.getBean(CTO51.class));
        sources.add(ApplicationContext.getBean(CNBeta.class));
        sources.add(ApplicationContext.getBean(CNR.class));
        sources.add(ApplicationContext.getBean(People.class));
        sources.add(ApplicationContext.getBean(IFeng.class));
        sources.add(ApplicationContext.getBean(WallStreetCN.class));
        sources.add(ApplicationContext.getBean(CSCOMCN.class));
        sources.add(ApplicationContext.getBean(SinaFinance.class));
    }
    public void execute() {
        this.statistic();
        messengers.clear();
        this.deleteDownloadedFiles();
        this.syncSavedArticles();
        WebDriver driver = webDriverLauncher.start();
        for (Source source : sources) {
            try {
                source.execute(driver);
            } catch (Exception e) {
                String message = String.format("Error found in executing: %s", this.getClass());
                logger.error(message, e);
                Messenger messenger = new Messenger(source.getClass().getName(), message);
                this.messengers.add(messenger);
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

    private void statistic() {
        Date date = new Date();
        String hour = DateFormatUtils.format(date, "E");
        if (!StringUtils.equalsIgnoreCase(hour, "Fri")) {
            return;
        }
        String logId = "Send_Message_" + DateFormatUtils.format(date, "yyyy-w-E");
        ActionLog log = dbManager.readById(logId, ActionLog.class);
        if (log != null) {
            return;
        }
        ApplicationContext.getBean(Statistics.class).execute();
    }

    @Deprecated
    private void deleteOldArticleLogs() {
        Date date = new Date();
        date = DateUtils.addDays(date, -3);
        String dateString = DateFormatUtils.format(date, "yyyy-MM-dd HH:mm:ss");
        String sql = String.format("DELETE FROM action_log WHERE lasttime < '%s'", dateString);
        dbManager.execute(sql);
    }

    /**
     * Read article original links from website via API,
     * save to DB in action_log table if not exists.
     */
    private void syncSavedArticles() {
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

    private void deleteDownloadedFiles() {
        File root = FileUtils.getFile(WebDriverLauncher.DOWNLOAD_PATH);
        if (!root.isDirectory()) {
            logger.error("Download directory not exists: {}", WebDriverLauncher.DOWNLOAD_PATH);
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
