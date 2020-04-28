package jp.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/4/23 15:27
 */
public class IgakuShoin extends Source {
    private static final int MAX_PAST_DAYS = 1;
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.igaku-shoin.co.jp/paperTop.do", Category.INTL_SCIENCE_JP);
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
        return new CSSQuery(".volume > li", "#serialinfo", "a", "",
            "", "#serialinfo > h3:first-child");
    }

    @Override
    protected int getSourceId() {
        return 18;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        this.parseTitleList(articles, list);
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
}
