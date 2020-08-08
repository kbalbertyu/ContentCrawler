package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import cn.btimes.utils.Common;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/8/7 23:16
 */
public class CYZone extends Source {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.cyzone.cn/", Category.ECONOMY);
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
        return new CSSQuery(".m-article-list", ".article-content", "a.item-title", "",
            "", ".time");
    }

    @Override
    protected int getSourceId() {
        return 1379;
    }

    @Override
    String getStatus() {
        return "3";
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        for (Element row : list) {
            Article article = this.parseDataTitleWithTimeText(row);
            if (article == null) break;
            this.parseCoverImage(row, article);

            articles.add(article);
        }
        return articles;
    }

    private void parseCoverImage(Element row, Article article) {
        Element imageElm = row.select(".pic-a > img").first();
        if (imageElm == null) return;
        String src = imageElm.attr("src");
        article.setCoverImage(Common.getAbsoluteUrl(src, driver.getCurrentUrl()));
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readContent(driver, article);
    }
}
