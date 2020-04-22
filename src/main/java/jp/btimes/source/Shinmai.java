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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/4/22 16:14
 */
public class Shinmai extends Source {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.shinmai.co.jp/news/nagano/web_keizai_list.html", Category.FINANCE_JP);
        URLS.put("https://www.shinmai.co.jp/news/nagano/web_shakai_list.html", Category.SOCIETY_JP);
        URLS.put("https://www.shinmai.co.jp/news/nagano/web_seiji_list.html", Category.SOCIETY_JP);
        URLS.put("https://www.shinmai.co.jp/news/nagano/web_sports_list.html", Category.GENERAL_JP);
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
