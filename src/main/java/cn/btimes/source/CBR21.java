package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/8/7 20:11
 */
public class CBR21 extends Source {

    private static final int MAX_PAST_DAYS = 2;
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.21cbr.com/", Category.ECONOMY);
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
        return new CSSQuery("div.item", ".content", ".tit > a", "",
            "", ".date");
    }

    @Override
    String getCoverSelector() {
        return ".pic img";
    }

    @Override
    protected int getSourceId() {
        return 1873;
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
    boolean ignorePastDateException() {
        return true;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readContent(driver, article);
    }

    @Override
    void removeDomNotInContentArea(Document doc) {
        super.removeDomNotInContentArea(doc);
        doc.select(".article-rel, .sidebar, .footer, span:contains(来源：21世纪商业评论)").remove();
    }

    @Override
    protected String getSourceName() {
        return "21商评网";
    }
}
