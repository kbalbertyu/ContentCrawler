package jp.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import org.jsoup.nodes.Document;
import org.openqa.selenium.WebDriver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/4/22 15:43
 */
public class Yahoo extends Source {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://news.yahoo.co.jp/categories/business", Category.FINANCE_JP);
        URLS.put("https://news.yahoo.co.jp/categories/life", Category.SOCIETY_JP);
        URLS.put("https://news.yahoo.co.jp/categories/world", Category.INTL_SCIENCE_JP);
        URLS.put("https://news.yahoo.co.jp/categories/it", Category.INTL_SCIENCE_JP);
        URLS.put("https://news.yahoo.co.jp/categories/science", Category.INTL_SCIENCE_JP);
        URLS.put("https://news.yahoo.co.jp/categories/sportss", Category.GENERAL_JP);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return null;
    }

    @Override
    protected String getDateFormat() {
        return null;
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return null;
    }

    @Override
    protected int getSourceId() {
        return 0;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        return null;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {

    }
}
