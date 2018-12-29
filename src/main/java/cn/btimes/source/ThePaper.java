package cn.btimes.source;

import cn.btimes.model.Article;
import cn.btimes.model.BTExceptions.PastDateException;
import cn.btimes.utils.Common;
import com.amzass.model.common.ActionLog;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.amzass.utils.common.PageUtils;
import com.amzass.utils.common.RegexUtils;
import com.amzass.utils.common.Tools;
import com.google.inject.Inject;
import com.kber.commons.DBManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 12/25/2018 1:07 AM
 */
public class ThePaper extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Inject private DBManager dbManager;

    private static final String BASE_URL = "https://www.thepaper.cn/";

    @Override
    protected String getUrl() {
        return "https://www.thepaper.cn/list_25434";
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = doc.select(".newsbox .news_li");
        for (Element row : list) {
            try {
                Article article = new Article();

                if (StringUtils.isBlank(row.html())) {
                    continue;
                }
                Element linkElm = row.select("h2 > a").get(0);
                article.setUrl(BASE_URL + linkElm.attr("href"));
                article.setTitle(linkElm.text());

                String timeText = HtmlParser.text(row, ".pdtt_trbs > span");
                article.setDate((this.parseDateText(timeText)));

                article.setSummary(HtmlParser.text(row, "p"));

                articles.add(article);
            } catch (PastDateException e) {
                logger.warn("Article that past {} minutes detected, complete the list fetching.", MAX_PAST_MINUTES);
                break;
            }
        }
        return articles;
    }

    private Date parseDateText(String timeText) {
        if (Tools.containsAny(timeText, "刚刚")) {
            return new Date();
        }
        if (!Tools.containsAny(timeText, "分钟", "秒", "刚刚")) {
            throw new PastDateException();
        }
        int minutes = NumberUtils.toInt(RegexUtils.getMatched(timeText, "\\d+"));
        if (minutes == 0) {
            throw new BusinessException("Unable to parse time text: " + timeText);
        }
        if (minutes > MAX_PAST_MINUTES) {
            throw new PastDateException();
        }
        return DateUtils.addMinutes(new Date(), -1 * minutes);
    }

    @Override
    protected Boolean validateLink(String href) {
        return null;
    }

    @Override
    public void execute(WebDriver driver) {
        driver.get(this.getUrl());

        // Scroll to bottom to make sure latest articles are loaded
        PageUtils.scrollToBottom(driver);
        WaitTime.Normal.execute();

        Document doc = Jsoup.parse(driver.getPageSource());

        List<Article> articles = this.parseList(doc);
        for (Article article : articles) {
            String logId = Common.toMD5(article.getUrl());
            ActionLog log = dbManager.readById(logId, ActionLog.class);
            if (log != null) {
                continue;
            }
            try {
                this.readArticle(driver, article);
                this.saveArticle(article, driver);
            } catch (PastDateException e) {
                logger.error("Article publish date has past {} minutes: {}", MAX_PAST_MINUTES, article.getUrl());
            } catch (BusinessException e) {
                logger.error("Unable to read article {}", article.getUrl(), e);
            }
            dbManager.save(new ActionLog(logId), ActionLog.class);
        }
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        driver.get(article.getUrl());
        WaitTime.Normal.execute();
        Document doc = Jsoup.parse(driver.getPageSource());

        article.setTitle(this.parseTitle(doc));
        article.setSource(this.parseSource(doc));

        Element contentElm = doc.select(".news_txt").first();
        article.setContent(this.cleanHtml(contentElm));

        Elements images = contentElm.select("img");
        List<String> contentImages = new ArrayList<>();
        for (Element image : images) {
            contentImages.add(image.attr("src"));
        }
        article.setContentImages(contentImages);
    }

    @Override
    protected Date parseDate(Document doc) {
        return null;
    }

    @Override
    protected void validateDate(Date date) {
    }

    @Override
    protected String parseTitle(Document doc) {
        return HtmlParser.text(doc, "h1.news_title");
    }

    @Override
    protected String parseSource(Document doc) {
        String sourceText = HtmlParser.text(doc, ".news_about > p > span:contains(来源)");
        return StringUtils.trim(StringUtils.remove(sourceText, "来源："));
    }

    @Override
    protected String parseContent(Document doc) {
        // Not used here
        return null;
    }

    @Override
    protected int getSourceId() {
        return 12;
    }

    public static void main(String[] args) {
        String content = Tools.readFileToString(FileUtils.getFile("C:/Work/Tmp/content.txt"));
        Document doc = Jsoup.parse(content);
        Element contentElm = doc.select(".news_txt").first();
        new ThePaper().cleanHtml(contentElm);
    }
}
