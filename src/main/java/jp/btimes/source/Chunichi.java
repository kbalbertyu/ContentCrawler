package jp.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/4/22 15:55
 */
public class Chunichi extends Source {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.chunichi.co.jp/s/article/economics.html", Category.FINANCE_JP);
        URLS.put("https://www.chunichi.co.jp/s/article/national.html", Category.SOCIETY_JP);
        URLS.put("https://www.chunichi.co.jp/s/article/politics.html", Category.SOCIETY_JP);
        URLS.put("https://www.chunichi.co.jp/s/article/world.html", Category.INTL_SCIENCE_JP);
        URLS.put("https://www.chunichi.co.jp/s/article/sports.html", Category.GENERAL_JP);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{1,2}月\\d{1,2}日 \\d{1,2}:\\d{1,2}";
    }

    @Override
    protected String getDateFormat() {
        return "MM'月'dd'日' HH:mm";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".Newslist > ul > li", ".News-textarea", "a", "",
            "", "a");
    }

    @Override
    protected int getSourceId() {
        return 6;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        this.parseDateTitleList(articles, list);
        articles.forEach(article -> {
            String title = StringUtils.removePattern(article.getTitle(), "\\(\\d{1,2}月\\d{1,2}日.+\\)");
            article.setTitle(title);
        });
        return articles;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readContent(driver, article);
    }

    @Override
    protected String cleanHtml(Element dom) {
        Elements elements = dom.select(".print");
        if (elements.size() > 0) {
            elements.remove();
        }
        return super.cleanHtml(dom);
    }
}
