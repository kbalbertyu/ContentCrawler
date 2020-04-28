package jp.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/4/23 8:46
 */
public class GifuNp extends Source {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.gifu-np.co.jp/news/business/", Category.FINANCE_JP);
        URLS.put("https://www.gifu-np.co.jp/news/national/", Category.SOCIETY_JP);
        URLS.put("https://www.gifu-np.co.jp/news/politics/", Category.SOCIETY_JP);
        URLS.put("https://www.gifu-np.co.jp/news/science/", Category.INTL_SCIENCE_JP);
        URLS.put("https://www.gifu-np.co.jp/news/medical/", Category.INTL_SCIENCE_JP);
        URLS.put("https://www.gifu-np.co.jp/news/tokyo2020/", Category.INTL_SCIENCE_JP);
        URLS.put("https://www.gifu-np.co.jp/news/life-culture/", Category.GENERAL_JP);
        URLS.put("https://www.gifu-np.co.jp/news/sports/", Category.GENERAL_JP);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}年\\d{1,2}月\\d{1,2}日 \\d{1,2}:\\d{1,2}";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy'年'MM'月'dd'日' HH:mm";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery("ul.archive > li", ".entry", ".title > a", "",
            "", ".day");
    }

    @Override
    protected int getSourceId() {
        return 14;
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
