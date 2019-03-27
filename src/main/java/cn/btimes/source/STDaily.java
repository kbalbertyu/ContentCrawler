package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-03-27 1:54 PM
 */
public class STDaily extends SourceWithoutDriver {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.stdaily.com/guoji/xinwen/kjxw.shtml", Category.FUTURE_INDUSTRIES);
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
        return new CSSQuery(".f_lieb_list > dl", ".content", "h3 > a", ".wenzi_box > p",
            "", ".dete");
    }

    @Override
    protected int getSourceId() {
        return 1588;
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
        Elements elements = dom.select("#indexNumber, .videoBox");
        if (elements.size() > 0) {
            elements.remove();
        }
        return super.cleanHtml(dom);
    }
}
