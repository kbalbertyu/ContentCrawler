package cn.btimes.source;

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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-02 3:41 PM
 */
public class NBD extends Source {

    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://finance.nbd.com.cn/", Category.FINANCE);
        URLS.put("http://economy.nbd.com.cn/", Category.ECONOMY);
        URLS.put("http://money.nbd.com.cn/", Category.FINANCE);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy-MM-dd HH:mm:ss";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery("ul.m-columnnews-list > li", ".g-articl-text", "a.f-title", ".g-article-abstract > p",
            "", ".f-source");
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
        this.readSummaryContent(driver, article);
    }

    @Override
    protected int getSourceId() {
        return 133;
    }

    @Override
    String cleanHtml(Element dom) {
        Elements elements = dom.select("p:contains(每经记者), p:contains(每经编辑)");
        if (elements.size() > 0) {
            elements.remove();
        }
        return super.cleanHtml(dom);
    }
}
