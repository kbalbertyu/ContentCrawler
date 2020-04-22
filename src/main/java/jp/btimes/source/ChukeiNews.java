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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/4/22 17:20
 */
public class ChukeiNews extends Source {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.chukei-news.co.jp/news/industry/c020/", Category.FINANCE_JP);
        URLS.put("https://www.chukei-news.co.jp/news/industry/c023/", Category.SOCIETY_JP);
        URLS.put("https://www.chukei-news.co.jp/news/industry/c001/", Category.INTL_SCIENCE_JP);
        URLS.put("https://www.chukei-news.co.jp/news/industry/c017/", Category.INTL_SCIENCE_JP);
        URLS.put("https://www.chukei-news.co.jp/news/industry/c004/", Category.INTL_SCIENCE_JP);
        URLS.put("https://www.chukei-news.co.jp/news/area/d000/", Category.GENERAL_JP);
        URLS.put("https://www.chukei-news.co.jp/news/area/d001/", Category.GENERAL_JP);
        URLS.put("https://www.chukei-news.co.jp/news/area/d002/", Category.GENERAL_JP);
        URLS.put("https://www.chukei-news.co.jp/news/area/d003/", Category.GENERAL_JP);
        URLS.put("https://www.chukei-news.co.jp/news/area/d004/", Category.GENERAL_JP);
        URLS.put("https://www.chukei-news.co.jp/news/area/d005/", Category.GENERAL_JP);
        URLS.put("https://www.chukei-news.co.jp/news/area/d006/", Category.GENERAL_JP);
        URLS.put("https://www.chukei-news.co.jp/news/area/d007/", Category.GENERAL_JP);
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
