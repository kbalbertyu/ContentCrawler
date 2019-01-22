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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-22 8:02 PM
 */
public class CNR extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String DATE_REGEX = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}";
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://travel.cnr.cn/2011lvpd/cjy/news/", Category.TRAVEL);
        URLS.put("http://travel.cnr.cn/hydt/", Category.TRAVEL);
        URLS.put("http://travel.cnr.cn/2011lvpd/gny/news/", Category.TRAVEL);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        String cssQuery = ".articleList > ul > li";
        this.checkArticleListExistence(doc, cssQuery);
        Elements list = doc.select(cssQuery);
        for (Element row : list) {
            try {
                Article article = new Article();
                String dateTextCssQuery = ".publishTime";
                this.checkDateTextExistence(row, dateTextCssQuery);
                String timeText = HtmlParser.text(row, dateTextCssQuery);
                article.setDate(this.parseDateText(timeText));

                String titleCssQuery = ".text > strong > a";
                this.checkTitleExistence(row, titleCssQuery);
                Element linkElm = row.select(titleCssQuery).get(0);
                article.setUrl(linkElm.attr("href"));
                article.setTitle(linkElm.text());

                String summaryCssQuery = ".text > p";
                this.checkSummaryExistence(row, summaryCssQuery);
                article.setSummary(HtmlParser.text(row, summaryCssQuery));

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

        article.setSource(this.parseSource(doc));

        String cssQuery = ".TRS_Editor";
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
        return null;
    }

    @Override
    protected String parseSource(Document doc) {
        return null;
    }

    @Override
    protected String parseContent(Document doc) {
        return null;
    }

    @Override
    protected int getSourceId() {
        return 229;
    }
}
