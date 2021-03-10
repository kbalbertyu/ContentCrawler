package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2021/3/9 8:57
 */
public class GPLP extends Source {
    private static final Map<String, Category> URLS = new HashMap<>();
    private static final int MAX_PAST_DAYS = 2;

    static {
        URLS.put("https://www.gplp.cn/", Category.ECONOMY);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}年\\d{1,2}月\\d{1,2}日";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy'年'MM'月'dd'日'";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".mvp-main-blog-story > li.infinite-post", "#mvp-content-main", ".mvp-main-blog-in > .mvp-main-blog-text > a", "",
            "", "time");
    }

    @Override
    protected int getSourceId() {
        return 1880;
    }

    @Override
    String getStatus() {
        return "3";
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        this.parseTitleList(articles, list);
        articles.removeIf(article -> StringUtils.contains(article.getTitle(), "视频"));
        return articles;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readDateContent(driver, article);
    }

    @Override
    protected Date parseDateText(String timeText) {
        return this.parseDateTextWithDay(timeText, this.getDateRegex(), this.getDateFormat(), MAX_PAST_DAYS);
    }

    @Override
    String getCoverSelector() {
        return "img";
    }

    @Override
    void removeDomNotInContentArea(Document doc) {
        super.removeDomNotInContentArea(doc);
        doc.select("strong:contains(来源：GPLP犀牛财经)").remove();
    }

    @Override
    protected String getSourceName() {
        return "犀牛财经";
    }
}
