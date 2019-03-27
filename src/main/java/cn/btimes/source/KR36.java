package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-03-27 12:52 PM
 */
public class KR36 extends SourceWithoutDriver {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://36kr.com/information/technology", Category.TECH);
        URLS.put("https://36kr.com/information/innovate", Category.TECH);
        URLS.put("https://36kr.com/information/real_estate", Category.REALESTATE);
        URLS.put("https://36kr.com/information/travel", Category.AUTO);
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
        return new CSSQuery(".kr-flow-article-item", ".articleDetailContent", ".article-item-title",
            ".article-item-description", "", ".kr-flow-bar-time");
    }

    @Override
    protected int getSourceId() {
        return 125;
    }

    @Override
    protected Date parseDateText(String timeText) {
        return this.parseDescribableDateText(timeText);
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
        Elements elements = dom.select("p:contains(编者按), p:contains(作者 |), p:contains(编辑 |)");
        if (elements.size() > 0) {
            elements.remove();
        }
        return super.cleanHtml(dom);
    }
}
