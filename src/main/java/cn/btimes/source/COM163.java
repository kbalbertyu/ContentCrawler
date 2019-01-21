package cn.btimes.source;

import cn.btimes.model.Article;
import cn.btimes.model.BTExceptions.PastDateException;
import cn.btimes.model.Category;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.PageLoadHelper.WaitTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-02 4:47 PM
 */
public class COM163 extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String DATE_REGEX = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://tech.163.com/gd/", Category.TECH);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        String cssQuery = "ul#news-flow-content > li";
        Elements list = doc.select(cssQuery);
        this.checkArticleListExistence(doc, cssQuery);
        for (Element row : list) {
            try {
                Article article = new Article();
                String dateTextCssQuery = ".sourceDate";
                this.checkDateTextExistence(row, dateTextCssQuery);
                String timeText = HtmlParser.text(row, dateTextCssQuery);
                article.setDate(this.parseDateText(timeText));

                String titleCssQuery = "h3 > a";
                this.checkTitleExistence(row, titleCssQuery);
                Element linkElm = row.select(titleCssQuery).get(0);
                article.setUrl(linkElm.attr("href"));
                article.setTitle(linkElm.text());

                String summaryCssQuery = ".newsDigest";
                this.checkSummaryExistence(row, summaryCssQuery);
                Element summaryElm = row.select(summaryCssQuery).first();
                summaryElm.select("a").remove();
                article.setSummary(summaryElm.text());

                articles.add(article);
            } catch (PastDateException e) {
                logger.warn("Article that past {} minutes detected, complete the list fetching.", MAX_PAST_MINUTES);
                break;
            }
        }
        return articles;
    }

    @Override
    protected Boolean validateLink(String href) {
        return null;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        driver.get(article.getUrl());
        WaitTime.Normal.execute();
        Document doc = Jsoup.parse(driver.getPageSource());

        article.setTitle(this.parseTitle(doc));
        article.setSource(this.parseSource(doc));

        String cssQuery = "#endText";
        this.checkArticleContentExistence(doc, cssQuery);
        Element contentElm = doc.select(cssQuery).first();
        article.setContent(this.cleanHtml(contentElm));
        this.fetchContentImages(article, contentElm);
    }

    @Override
    protected Date parseDateText(String timeText) {
        return this.parseDateText(timeText, DATE_REGEX, DATE_FORMAT);
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
        String cssQuery = "#epContentLeft > h1";
        this.checkTitleExistence(doc, cssQuery);
        return HtmlParser.text(doc, cssQuery);
    }

    @Override
    protected String parseSource(Document doc) {
        String cssQuery = "#ne_article_source";
        this.checkSourceExistence(doc, cssQuery);
        return HtmlParser.text(doc, cssQuery);
    }

    @Override
    protected String parseContent(Document doc) {
        return null;
    }

    @Override
    protected int getSourceId() {
        return 211;
    }

    @Override
    String cleanHtml(Element dom) {
        dom.select(".ep-source, [adtype], [class^=gg], .otitle").remove();
        return super.cleanHtml(dom);
    }
}
