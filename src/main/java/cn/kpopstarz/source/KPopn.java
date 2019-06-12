package cn.kpopstarz.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.BTExceptions.PastDateException;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import cn.btimes.source.SourceWithoutDriver;
import com.amzass.utils.common.Constants;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/6/12 20:19
 */
public class KPopn extends SourceWithoutDriver {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.kpopn.com/category/news/artist", Category.General);
        URLS.put("https://www.kpopn.com/category/news/music", Category.General);
        URLS.put("https://www.kpopn.com/category/news/tv", Category.General);
        URLS.put("https://www.kpopn.com/category/news/print-media", Category.General);
        URLS.put("https://www.kpopn.com/category/news/charts", Category.General);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy-MM-dd HH:mm:ss";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".post-list-outer", ".single-body > .entry", ".post-title > a", ".post-excerpt",
            "", "time");
    }

    @Override
    protected int getSourceId() {
        return 0;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        int i = 0;
        for (Element row : list) {
            try {
                Article article = new Article();
                String dateTextCssQuery = getCSSQuery().getTime();
                this.checkDateTextExistence(row, dateTextCssQuery);
                String timeText = row.select(dateTextCssQuery).get(0).attr("datetime");
                Date date = this.parseDateText(timeText);
                article.setDate(date);

                this.parseTitle(row, article);
                this.parseSummary(row, article);
                articles.add(article);
            } catch (PastDateException e) {
                if (i++ < Constants.MAX_REPEAT_TIMES) {
                    continue;
                }
                logger.warn("Article that past {} minutes detected, complete the list fetching: ", config.getMaxPastMinutes(), e);
                break;
            }
        }
        return articles;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readContent(driver, article);
    }

    @Override
    protected String cleanHtml(Element dom) {
        Elements elements = dom.select("div.single-ads, p:contains(如若轉載請把), p:contains(轉載請註明)");
        if (elements.size() > 0) {
            elements.remove();
        }
        return super.cleanHtml(dom);
    }
}
