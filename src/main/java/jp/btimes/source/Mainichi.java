package jp.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/4/19 22:29
 */
public class Mainichi extends Source {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://mainichi.jp/biz/", Category.FINANCE_JP);
        URLS.put("https://mainichi.jp/shakai/", Category.SOCIETY_JP);
        URLS.put("https://mainichi.jp/seiji/", Category.SOCIETY_JP);
        URLS.put("https://mainichi.jp/world/", Category.INTL_SCIENCE_JP);
        URLS.put("https://mainichi.jp/opinion/", Category.INTL_SCIENCE_JP);
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
        return new CSSQuery(".list-typeD > li", ".main-text", "a:first-child", "",
            "", "time");
    }

    @Override
    protected int getSourceId() {
        return 26;
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
        this.readContent(driver, article);
    }

    @Override
    protected String cleanHtml(Element dom) {
        Elements elements = dom.select(".ad, .no-print");
        if (elements.size() > 0) {
            elements.remove();
        }
        return super.cleanHtml(dom);
    }
}
