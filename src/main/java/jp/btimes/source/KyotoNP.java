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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/4/22 16:56
 */
public class KyotoNP extends Source {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.kyoto-np.co.jp/category/news-original/経済", Category.FINANCE_JP);
        URLS.put("https://www.kyoto-np.co.jp/category/news-original/社会", Category.SOCIETY_JP);
        URLS.put("https://www.kyoto-np.co.jp/category/news-original/政治", Category.SOCIETY_JP);
        URLS.put("https://www.kyoto-np.co.jp/category/news-original/文化・ライフ", Category.GENERAL_JP);
        URLS.put("https://www.kyoto-np.co.jp/subcategory/京都府", Category.GENERAL_JP);
        URLS.put("https://www.kyoto-np.co.jp/subcategory/滋賀県", Category.GENERAL_JP);
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
