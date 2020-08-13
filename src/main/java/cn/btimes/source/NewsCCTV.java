package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/8/13 10:15
 */
public class NewsCCTV extends Source {

    private static final int MAX_PAST_DAYS = 1;
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://news.cctv.com/", Category.ECONOMY);
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
        return new CSSQuery("#newslist > li", "#content_area", "h3.title > a", "",
            "", ".date");
    }

    @Override
    String getCoverSelector() {
        return ".image > a > img";
    }

    @Override
    protected int getSourceId() {
        return 197;
    }

    @Override
    String getStatus() {
        return "3";
    }

    @Override
    protected Date parseDateText(String timeText) {
        return this.parseDateTextWithDay(timeText, this.getDateRegex(), this.getDateFormat(), MAX_PAST_DAYS);
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        this.parseDateTitleList(articles, list);
        return articles;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readContent(driver, article);
    }
}
