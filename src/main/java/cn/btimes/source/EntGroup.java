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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-03 11:48 AM
 */
public class EntGroup extends Source {
    private static final int MAX_PAST_DAYS = 0;
    private static final String DATE_REGEX = "\\d{4}-\\d{2}-\\d{2}";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String BASE_URL = "http://www.entgroup.cn";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.entgroup.cn/", Category.ENTERTAINMENT);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = doc.select("div.articlebox");
        for (Element row : list) {
            try {
                Article article = new Article();
                String timeText = HtmlParser.text(row, ".movetit");
                article.setDate(this.parseDateText(timeText));

                Element linkElm = row.select("h1 > a").get(0);
                article.setUrl(BASE_URL + linkElm.attr("href"));
                article.setTitle(linkElm.text());

                article.setSummary(HtmlParser.text(doc, ".contbox > p"));

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

        Element contentElm = doc.select(".detailsbox").first();
        article.setContent(this.cleanHtml(contentElm));
        this.fetchContentImages(article, contentElm);
    }

    @Override
    protected Date parseDateText(String timeText) {
        return this.parseDateTextWithDay(timeText, DATE_REGEX, DATE_FORMAT, MAX_PAST_DAYS);
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
        return 1169;
    }

    @Override
    String cleanHtml(Element dom) {
        dom.select("h1, .biaoqian, .zhaiyao, .writer, [id*=baidu_bookmark]").remove();
        return super.cleanHtml(dom);
    }
}
