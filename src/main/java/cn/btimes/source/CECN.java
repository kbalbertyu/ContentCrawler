package cn.btimes.source;

import cn.btimes.model.Article;
import cn.btimes.model.BTExceptions.PastDateException;
import cn.btimes.utils.Common;
import com.amzass.model.common.ActionLog;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.amzass.utils.common.Tools;
import com.google.inject.Inject;
import com.kber.commons.DBManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 12/24/2018 6:30 PM
 */
public class CECN extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final static String URL = "http://www.ce.cn/";
    private static final String[] DATE_PATTERNS = {"yyyy'年'MM'月'dd'日' HH:mm", "yyyy'年'MM'月'dd'日", "MM'月'dd'日", "HH:mm"};

    @Inject DBManager dbManager;

    public void execute(WebDriver driver) {
        driver.get(this.getUrl());
        WaitTime.Normal.execute();
        Document doc = Jsoup.parse(driver.getPageSource());
        List<Article> articles = this.parseList(doc);
        for (Article article : articles) {
            String link = article.getUrl();
            String logId = Common.toMD5(link);
            ActionLog log = dbManager.readById(logId, ActionLog.class);
            if (log != null) {
                continue;
            }
            try {
                this.readArticle(driver, article);
                this.saveArticle(article, driver);
            } catch (PastDateException e) {
                logger.error("Article publish date has past 30 minutes: {}", link);
            } catch (BusinessException e) {
                logger.error("Unable to read article {}", link, e);
            }
            dbManager.save(new ActionLog(logId), ActionLog.class);
        }
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        driver.get(article.getUrl());
        WaitTime.Normal.execute();
        Document doc = Jsoup.parse(driver.getPageSource());

        article.setDate(this.parseDate(doc));
        article.setTitle(this.parseTitle(doc));
        article.setSource(this.parseSource(doc));
        article.setContent(this.parseContent(doc));
    }

    @Override
    protected Date parseDate(Document doc) {
        String dateText = HtmlParser.text(doc, "#articleTime");
        Date date;
        try {
            date = DateUtils.parseDate(dateText, Locale.PRC, DATE_PATTERNS);
        } catch (ParseException e) {
            throw new BusinessException(String.format("Unable to parse date: %s", dateText));
        }
        this.validateDate(date);
        return date;
    }

    @Override
    protected void validateDate(Date date) {
        int diff = Minutes.minutesBetween(new DateTime(date), DateTime.now()).getMinutes();
        if (diff >= 30) {
            throw new PastDateException();
        }
    }

    @Override
    protected String parseTitle(Document doc) {
        return StringUtils.trim(HtmlParser.text(doc, "h1#articleTitle"));
    }

    @Override
    protected String parseSource(Document doc) {
        return StringUtils.trim(StringUtils.remove(HtmlParser.text(doc, "#articleSource"), "来源："));
    }

    @Override
    protected String parseContent(Document doc) {
        Element element = doc.select("#articleText .TRS_Editor").get(0);
        return element.html();
    }

    @Override
    protected int getSourceId() {
        return 0;
    }

    @Override
    protected String getUrl() {
        return URL;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements rows = doc.select("a");
        for (Element row : rows) {
            String link = StringUtils.trim(row.attr("href"));
            if (!this.validateLink(link)) {
                continue;
            }
            Article article = new Article();
            article.setUrl(link);
            articles.add(article);
        }
        return articles;
    }

    @Override
    protected Boolean validateLink(String href) {
        return StringUtils.isNotBlank(href) &&
            Tools.contains(href, "shtml", "ce.cn") &&
            !Tools.containsAny(href, "index.shtml", "/about",
                "t20080311_14791942.shtml",
                "t20130115_24030051.shtml",
                "t20180118_27795972.shtml") &&
            StringUtils.length(href) < 100;
    }
}
