package cn.btimes.service;

import cn.btimes.model.common.Config;
import cn.btimes.model.news.NewsFlow;
import cn.btimes.model.news.NewsFlowSource;
import cn.btimes.utils.Common;
import cn.btimes.utils.PageUtils;
import com.alibaba.fastjson.JSONObject;
import com.amzass.model.common.ActionLog;
import com.amzass.service.common.ApplicationContext;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.google.inject.Inject;
import com.kber.commons.DBManager;
import com.mailman.model.common.WebApiResult;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/6/29 10:04
 */
public class NewsFlowCrawler implements ServiceExecutorInterface {
    private static final String SOURCE_URL = "https://www.ushknews.com/";
    private static final String SOURCE = NewsFlowSource.USHK.source;
    private final Logger logger = LoggerFactory.getLogger(TagGenerator.class);
    @Inject private ApiRequest apiRequest;
    @Inject private DBManager dbManager;
    @Inject private WebDriverLauncher webDriverLauncher;

    @Override
    public void execute(Config config) {
        this.syncSavedLogs(config);
        WebDriver driver = webDriverLauncher.startWithoutLogin(config.getApplication().name());
        Document doc = this.loadSourcePage(driver);
        List<NewsFlow> items = this.crawlItems(doc);
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        this.saveItems(items, config);
    }

    private void saveItems(List<NewsFlow> items, Config config) {
        WebApiResult result = apiRequest.post("/article/importNewsFlowItems", JSONObject.toJSONString(items), config);
        System.out.println(result.getData());
    }

    private List<NewsFlow> crawlItems(Document doc) {
        Elements groups = doc.select(".ushk-flash__group");
        if (groups.size() == 0) {
            return null;
        }

        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);

        List<NewsFlow> items = new ArrayList<>();
        String source = NewsFlowSource.USHK.source;

        for (Element group : groups) {
            String day = HtmlParser.text(group, ".ushk-flash__date > dl > dt");
            String month = HtmlParser.text(group, ".ushk-flash__date > dl > dd");
            month = StringUtils.remove(month, "月");
            String date = String.format("%d-%s-%s", year, month, day);

            Elements rows = group.select(".ushk-flash_item");
            for (Element row : rows) {
                String title = HtmlParser.text(row, ".ushk-flash_text-box");
                String time = HtmlParser.text(row, ".ushk-flash_time");
                String dateTime = date + " " + time;
                if (this.logExists(source, dateTime)) {
                    continue;
                }

                NewsFlow flow = new NewsFlow();
                flow.setTitle(title);
                flow.setSource(SOURCE);
                flow.setDate(dateTime);
                items.add(flow);
            }
        }

        return items;
    }

    private boolean logExists(String source, String dateTime) {
        String logId = "nf:" + source + "-" + dateTime;
        ActionLog log = dbManager.readById(logId, ActionLog.class);
        return log != null;
    }

    private Document loadSourcePage(WebDriver driver) {
        driver.get(SOURCE_URL);
        WaitTime.Normal.execute();

        while (true) {
            PageUtils.scrollToBottom(driver);
            PageUtils.click(driver, By.cssSelector("a.ushk-more__btn"));
            WaitTime.Normal.execute();

            Document doc = Jsoup.parse(driver.getPageSource());
            Elements rows = doc.select(".ushk-flash__group");
            if (rows.size() >= 4) {
                break;
            }
        }

        return Jsoup.parse(driver.getPageSource());
    }

    private void syncSavedLogs(Config config) {
        String url = String.format("/article/fetchRecentNewsFlowItems?source=%s", SOURCE);
        WebApiResult result = apiRequest.get(url, config);
        if (StringUtils.equals(result.getCode(), "0")) {
            logger.error("Unable to fetch news flow: {}", result.getMessage());
            return;
        }
        List<String> items = JSONObject.parseArray(result.getData(), String.class);
        if (items.size() == 0) {
            logger.warn("No news flow fetched.");
            return;
        }
        for (String logId : items) {
            ActionLog log = dbManager.readById(logId, ActionLog.class);
            if (log != null) {
                continue;
            }
            dbManager.save(new ActionLog(logId), ActionLog.class);
        }
    }

    public static void main(String[] args) {
        String appName = args.length > 0 ? args[0] : null;
        Config config = Common.loadApplicationConfig(appName);
        ApplicationContext.getBean(NewsFlowCrawler.class).execute(config);
        System.exit(0);
    }
}
