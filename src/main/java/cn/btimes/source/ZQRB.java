package cn.btimes.source;

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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/8/7 21:04
 */
public class ZQRB extends Source {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.zqrb.cn/yuanchuangnews/index_p1.html", Category.ECONOMY);
        URLS.put("http://www.zqrb.cn/yuanchuangnews/index_p2.html", Category.ECONOMY);
        URLS.put("http://www.zqrb.cn/yuanchuangnews/index_p3.html", Category.ECONOMY);
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
        return new CSSQuery(".news_content > ul > li", ".content-lcq", "a", "",
            "", ".date");
    }

    @Override
    protected int getSourceId() {
        return 130;
    }

    @Override
    String getStatus() {
        return "3";
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
