package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-22 8:02 PM
 */
public class CNR extends Source {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://travel.cnr.cn/2011lvpd/cjy/news/", Category.TRAVEL);
        URLS.put("http://travel.cnr.cn/hydt/", Category.TRAVEL);
        URLS.put("http://travel.cnr.cn/2011lvpd/gny/news/", Category.TRAVEL);
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
    String removeSourceNoise(String source) {
        return StringUtils.substringAfter(source, "来源：");
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".articleList > ul > li", ".TRS_Editor", ".text > strong > a",
            ".text > p", ".subject > .source > span:contains(来源：)", ".publishTime");
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
        this.readContentSource(driver, article);
    }

    @Override
    protected int getSourceId() {
        return 229;
    }
}
