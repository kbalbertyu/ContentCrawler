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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-22 8:36 PM
 */
public class People extends Source {

    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://house.people.com.cn/GB/194441/index.html", Category.REALESTATE);
        URLS.put("http://money.people.com.cn/GB/218900/index.html", Category.FINANCE);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{2}月\\d{2}日 \\d{2}:\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "MM'月'dd'日 'HH:mm";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".p2j_list > ul.list_14 > li", "#rwb_zw", "a", "",
            "", ".gray");
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        this.parseDateTitleList(articles, list);
        return articles;
    }

    @Override
    protected String cleanHtml(Element dom) {
        Elements elements = dom.select(".edit");
        if (elements.size() > 0) {
            elements.remove();
        }
        return super.cleanHtml(dom);
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readContent(driver, article);
    }

    @Override
    protected int getSourceId() {
        return 33;
    }

    @Override
    String getStatus() {
        return "3";
    }
}
