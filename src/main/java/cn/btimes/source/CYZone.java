package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import cn.btimes.utils.Tools;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import sun.swing.StringUIClientPropertyKey;

import javax.tools.Tool;
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
    String getCoverSelector() {
        return ".pic-a > img";
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
            Article article = this.parseDateTitleWithTimeText(row);
            if (article == null) break;
            articles.add(article);
        }
        return articles;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readContent(driver, article);
    }

    @Override
    void removeDomNotInContentArea(Document doc) {
        super.removeDomNotInContentArea(doc);
        doc.select(".article-tags, .share-collect, #cyzone-comments-main, .recommend-warp, .m-def-main-right").remove();
    }

    @Override
    boolean isOriginal(Document doc) {
        return StringUtils.contains(doc.html(), "本文为创业邦原创");
    }

    @Override
    protected String getSourceName() {
        return "创业邦";
    }
}
