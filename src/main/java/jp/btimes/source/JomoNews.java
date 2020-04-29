package jp.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/4/23 16:17
 */
public class JomoNews extends Source {
    private static final int MAX_PAST_DAYS = 1;
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.jomo-news.co.jp/sports", Category.GENERAL_JP);
        URLS.put("https://www.jomo-news.co.jp/life", Category.GENERAL_JP);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}/\\d{1,2}/\\d{1,2}";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy/MM/dd";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery("ul.article-list > li", ".article-main > .article-body", "a", "a .article-ttl",
            "", ".article-date");
    }

    @Override
    protected int getSourceId() {
        return 22;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        this.parseDateTitleSummaryList(articles, list);
        articles.forEach(article -> {
            article.setTitle(article.getSummary());
            article.setSummary("");
        });
        return articles;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readContent(driver, article);
    }

    @Override
    protected Date parseDateText(String timeText) {
        return this.parseDateTextWithDay(timeText, this.getDateRegex(), this.getDateFormat(), MAX_PAST_DAYS);
    }
}
