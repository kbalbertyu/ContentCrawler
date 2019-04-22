package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.BTExceptions.PastDateException;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import com.amzass.service.sellerhunt.HtmlParser;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-04 7:10 AM
 */
public class JieMian extends SourceWithoutDriver {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.jiemian.com/lists/63.html", Category.ENTERTAINMENT);
        URLS.put("https://www.jiemian.com/lists/42.html", Category.LIFESTYLE);
        URLS.put("https://www.jiemian.com/lists/2.html", Category.COMPANY);
        URLS.put("https://www.jiemian.com/lists/82.html", Category.SPORTS);
        URLS.put("https://www.jiemian.com/lists/105.html", Category.TRAVEL);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy/MM/dd HH:mm";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".news-list > .list-view > .news-view", ".article-main", "h3 > a",
            ".article-header > p", ".article-source > p:contains(来源：)", ".article-info");
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        for (Element row : list) {
            try {
                Article article = new Article();
                this.checkDateTextExistence(row, ".date");
                String timeText = HtmlParser.text(row, ".date");
                this.parseDescribableDateText(timeText);

                this.parseTitle(row, article);
                articles.add(article);
            } catch (PastDateException e) {
                logger.warn("Article that past {} minutes detected, complete the list fetching: ",
                    config.getMaxPastMinutes(), e);
                break;
            }
        }
        return articles;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readDateSummaryContent(driver, article);
    }

    @Override
    protected int getSourceId() {
        return 105;
    }

    @Override
    protected String cleanHtml(Element dom) {
        Elements elements = dom.select("#ad-content, p:contains(编辑：)");
        if (elements.size() > 0) {
            elements.remove();
        }
        return super.cleanHtml(dom);
    }
}
