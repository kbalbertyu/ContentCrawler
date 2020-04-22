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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/4/20 8:05
 */
public class TokyoNp extends Source {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.tokyo-np.co.jp/s/article/economics.html", Category.FINANCE_JP);
        URLS.put("https://www.tokyo-np.co.jp/article/economics/list/", Category.FINANCE_JP);
        URLS.put("https://www.tokyo-np.co.jp/article/economics/economic_confe/", Category.FINANCE_JP);
        URLS.put("https://www.tokyo-np.co.jp/s/article/national.html", Category.SOCIETY_JP);
        URLS.put("https://www.tokyo-np.co.jp/article/national/list/", Category.SOCIETY_JP);
        URLS.put("https://www.tokyo-np.co.jp/s/article/culture.html", Category.SOCIETY_JP);
        URLS.put("https://www.tokyo-np.co.jp/article/national/obituaries/index.html", Category.SOCIETY_JP);
        URLS.put("https://www.tokyo-np.co.jp/s/article/world.html", Category.INTL_SCIENCE_JP);
        URLS.put("https://www.tokyo-np.co.jp/article/world/list/", Category.INTL_SCIENCE_JP);
        URLS.put("https://www.tokyo-np.co.jp/s/article/sports.html", Category.GENERAL_JP);
        URLS.put("https://www.tokyo-np.co.jp/article/sports/list/", Category.GENERAL_JP);
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
