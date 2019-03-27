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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-03-27 1:29 PM
 */
public class HuanQiuTech extends SourceWithoutDriver {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://tech.huanqiu.com/it/?agt=15438", Category.TECH);
        URLS.put("http://tech.huanqiu.com/science/?agt=15438", Category.TECH);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy-MM-dd HH:mm";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery("ul.listPicBox > li.item", ".la_con", "h3 > a", "h5",
            "", "h6");
    }

    @Override
    protected int getSourceId() {
        return 1357;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        this.parseDateTitleSummaryList(articles, list);
        return articles;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readContent(driver, article);
    }

    @Override
    protected String cleanHtml(Element dom) {
        Elements elements = dom.select("[id^=ad], #pages");
        if (elements.size() > 0) {
            elements.remove();
        }
        return super.cleanHtml(dom);
    }
}
