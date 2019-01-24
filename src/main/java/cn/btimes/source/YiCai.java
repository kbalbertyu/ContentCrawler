package cn.btimes.source;

import cn.btimes.model.Article;
import cn.btimes.model.BTExceptions.PastDateException;
import cn.btimes.model.CSSQuery;
import cn.btimes.model.Category;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-02 10:03 AM
 */
public class YiCai extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.yicai.com/news/hongguan/", Category.ECONOMY);
        URLS.put("https://www.yicai.com/news/quyu/", Category.ECONOMY);
        URLS.put("https://www.yicai.com/news/jinrong/", Category.FINANCE);
        URLS.put("https://www.yicai.com/news/gushi/", Category.FINANCE);
        URLS.put("https://www.yicai.com/news/dafengwenhua/", Category.ENTERTAINMENT);
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
        return new CSSQuery("#newslist > a.f-db", ".m-txt", "h2", ".intro",
            "", ".author > span");
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        for (Element row : list) {
            try {
                Article article = new Article();
                this.parseDate(row, article);
                this.parseTitle(row, article);
                article.setUrl(row.attr("href"));

                articles.add(article);
            } catch (PastDateException e) {
                logger.warn("Article that past {} minutes detected, complete the list fetching.", MAX_PAST_MINUTES);
                break;
            }
        }
        return articles;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readSummaryContent(driver, article);
    }

    @Override
    protected int getSourceId() {
        return 5;
    }

    @Override
    String cleanHtml(Element dom) {
        dom.select("h3.f-tar").remove();
        return super.cleanHtml(dom);
    }
}
