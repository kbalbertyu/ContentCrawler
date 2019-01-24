package cn.btimes.source;

import cn.btimes.model.Article;
import cn.btimes.model.CSSQuery;
import cn.btimes.model.Category;
import com.amzass.utils.PageLoadHelper.WaitTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-03 7:44 PM
 */
public class CCDY extends Source {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.ccdy.cn/", Category.ENTERTAINMENT);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{2}-\\d{2} \\d{2}:\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "MM-dd HH:mm";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".con > ul.pic1 > li", ".TRS_Editor", "h3 > a",
            ".pl1 > p:eq(0)", "", ".pl1");
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        this.parseDateTitleSummaryList(articles, list);
        return articles;
    }


    @Override
    String cleanHtml(Element dom) {
        dom.select("style, p:contains(中国文化传媒网融媒体记者)").remove();
        return super.cleanHtml(dom);
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        driver.get(article.getUrl());
        WaitTime.Normal.execute();
        Document doc = Jsoup.parse(driver.getPageSource());
        this.parseContent(doc, article);
    }

    @Override
    protected int getSourceId() {
        return 1142;
    }
}
