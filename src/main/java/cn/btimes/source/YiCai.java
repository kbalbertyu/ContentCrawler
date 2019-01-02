package cn.btimes.source;

import cn.btimes.model.Article;
import cn.btimes.model.BTExceptions.PastDateException;
import cn.btimes.model.Category;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.PageLoadHelper.WaitTime;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-02 10:03 AM
 */
public class YiCai extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String DATE_REGEX = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}";
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";
    private static final String BASE_URL = "https://www.yicai.com";
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.yicai.com/news/hongguan/", Category.ECONOMY);
        URLS.put("https://www.yicai.com/news/quyu/", Category.ECONOMY);
        URLS.put("https://www.yicai.com/news/jinrong/", Category.FINANCE);
        URLS.put("https://www.yicai.com/news/gushi/", Category.FINANCE);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = doc.select("#newslist > a.f-db");
        for (Element row : list) {
            try {
                Article article = new Article();
                String timeText = HtmlParser.text(row, ".author > span");
                article.setDate(this.parseDateText(timeText));

                article.setUrl(BASE_URL + row.attr("href"));
                Element titleElm = row.select("h2").get(0);
                article.setTitle(titleElm.text());

                article.setSummary(HtmlParser.text(row, "p:eq(0)"));

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
        Element contentElm = doc.select(".m-txt").first();
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
        return StringUtils.EMPTY;
    }

    @Override
    protected String parseContent(Document doc) {
        return StringUtils.EMPTY;
    }

    @Override
    protected int getSourceId() {
        return 5;
    }

    @Override
    String cleanHtml(Element dom) {
        dom.select("h3.f-tar").remove();
        return super.cleanHtml(dom);
    }
}
