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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-02 3:41 PM
 */
public class NBD extends Source {
    private static final String DATE_REGEX = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://finance.nbd.com.cn/", Category.FINANCE);
        URLS.put("http://economy.nbd.com.cn/", Category.ECONOMY);
        URLS.put("http://money.nbd.com.cn/", Category.FINANCE);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = doc.select("ul.m-columnnews-list > li");
        for (Element row : list) {
            try {
                Article article = new Article();
                String timeText = HtmlParser.text(row, ".f-source");
                article.setDate((this.parseDateText(timeText)));

                Element linkElm = row.select("a.f-title").get(0);
                article.setUrl(linkElm.attr("href"));
                article.setTitle(linkElm.text());

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

        String summary = HtmlParser.text(doc, ".g-article-abstract > p");
        if (StringUtils.isNotBlank(summary)) {
            article.setSummary(summary);
        }

        Element contentElm = doc.select(".g-articl-text").first();
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
        return null;
    }

    @Override
    protected int getSourceId() {
        return 133;
    }

    @Override
    String cleanHtml(Element dom) {
        dom.select("p:contains(每经记者), p:contains(每经编辑)").remove();
        return super.cleanHtml(dom);
    }
}
