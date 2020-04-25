package jp.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/4/23 0:08
 */
public class Bci extends Source {
    private static final int MAX_PAST_DAYS = 1;
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.bci.co.jp/news", Category.FINANCE_JP);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{2}/\\d{2}/\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "yy/MM/dd";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".newsTopList > ul > li", "article", "a", "",
            "", "a");
    }

    @Override
    protected int getSourceId() {
        return 3;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        this.parseDateTitleList(articles, list);
        articles.forEach(article -> {
            String title = StringUtils.removePattern(article.getTitle(), "\\('\\d{2}/\\d{2}/\\d{2}\\)");
            article.setTitle(title);
        });
        return articles;
    }

    @Override
    protected Date parseDateText(String timeText) {
        return this.parseDateTextWithDay(timeText, this.getDateRegex(), this.getDateFormat(), MAX_PAST_DAYS);
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readContent(driver, article);
    }
}
