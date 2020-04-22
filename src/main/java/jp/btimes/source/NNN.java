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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/4/22 16:51
 */
public class NNN extends Source {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.nnn.co.jp/knews/economics.html", Category.FINANCE_JP);
        URLS.put("https://www.nnn.co.jp/knews/national.html", Category.SOCIETY_JP);
        URLS.put("https://www.nnn.co.jp/knews/politics.html", Category.SOCIETY_JP);
        URLS.put("https://www.nnn.co.jp/knews/world.html", Category.INTL_SCIENCE_JP);
        URLS.put("https://www.nnn.co.jp/knews/science.html", Category.INTL_SCIENCE_JP);
        URLS.put("https://www.nnn.co.jp/knews/sports.html", Category.GENERAL_JP);
        URLS.put("https://www.nnn.co.jp/knews/culture.html", Category.GENERAL_JP);
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
