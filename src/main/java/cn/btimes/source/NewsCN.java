package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.BTExceptions.PastDateException;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import com.amzass.utils.common.Constants;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/7/17 5:27
 */
public class NewsCN extends Source {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final int MAX_PAST_DAYS = 1;
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.news.cn/tech/qqbb.htm", Category.TECH);
        URLS.put("http://www.news.cn/auto/jsxx.htm", Category.AUTO);
        URLS.put("http://www.xinhuanet.com/house/24xsjx.htm", Category.REALESTATE);
        URLS.put("http://www.news.cn/money/jrlb.htm", Category.FINANCE);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}-\\d{2}-\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy-MM-dd";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery("#showData0 > li", "#p-detail", "h3 > a", "",
            "", ".time");
    }

    @Override
    protected int getSourceId() {
        return 7;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        int i = 0;
        for (Element row : list) {
            if (row.hasClass("moreBtn")) {
                continue;
            }
            try {
                Article article = new Article();
                this.parseTitle(row, article);
                this.parseDate(row, article);
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
    protected Date parseDateText(String timeText) {
        return this.parseDateTextWithDay(timeText, this.getDateRegex(), this.getDateFormat(), MAX_PAST_DAYS);
    }

    @Override
    protected String cleanHtml(Element dom) {
        Elements elements = dom.select(".tadd, .p-tags");
        if (elements.size() > 0) {
            elements.remove();
        }
        return super.cleanHtml(dom);
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readContent(driver, article);
    }
}
