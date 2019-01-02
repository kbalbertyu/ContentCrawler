package cn.btimes.source;

import cn.btimes.model.Article;
import cn.btimes.model.BTExceptions.PastDateException;
import cn.btimes.model.Category;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Tools;
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
        URLS.put("http://tech.163.com/", Category.TECH);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = doc.select("li.newsdata_item > .ndi_main > .news_article");
        for (Element row : list) {
            try {
                Article article = new Article();
                String timeText = HtmlParser.text(row, "span.time");
                if (Tools.containsAny(timeText, "小时", "天")) {
                    throw new PastDateException();
                }

                Element linkElm = row.select("h3 > a").get(0);
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

        String timeText = HtmlParser.text(doc, ".post_time_source");
        article.setDate(this.parseDateText(timeText));

        article.setSource(this.parseSource(doc));

        Element contentElm = doc.select("#endText").first();
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
        return HtmlParser.text(doc, "#ne_article_source");
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
        dom.select(".ep-source, [adtype], [class^=gg]").remove();
        return super.cleanHtml(dom);
    }
}
