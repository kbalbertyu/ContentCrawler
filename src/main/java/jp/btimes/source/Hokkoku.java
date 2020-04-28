package jp.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/4/22 16:48
 */
public class Hokkoku extends Source {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.hokkoku.co.jp/index.php?genre=keizai", Category.FINANCE_JP);
        URLS.put("https://www.hokkoku.co.jp/index.php?genre=syakai", Category.SOCIETY_JP);
        URLS.put("https://www.hokkoku.co.jp/index.php?genre=seiji", Category.SOCIETY_JP);
        URLS.put("https://www.hokkoku.co.jp/index.php?genre=kokusai", Category.INTL_SCIENCE_JP);
        URLS.put("https://www.hokkoku.co.jp/index.php?genre=sports", Category.GENERAL_JP);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{1,2}/\\d{1,2} \\d{1,2}:\\d{1,2}";
    }

    @Override
    protected String getDateFormat() {
        return "MM/dd HH:mm";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".newstop > li", ".tp_sub_detailarea_body", "a", "",
            "", "a");
    }

    @Override
    protected int getSourceId() {
        return 17;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        this.parseDateTitleList(articles, list);
        articles.forEach(article -> {
            String title = StringUtils.removePattern(article.getTitle(), "\\(\\d{1,2}/\\d{1,2} \\d{1,2}:\\d{1,2}\\)");
            article.setTitle(title.trim());
        });
        return articles;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readContent(driver, article);
    }
}
