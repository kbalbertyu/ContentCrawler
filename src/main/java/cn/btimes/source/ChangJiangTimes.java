package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import com.amzass.service.sellerhunt.HtmlParser;
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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/8/21 13:36
 */
public class ChangJiangTimes extends SourceWithoutDriver {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.changjiangtimes.com/c/4378/", Category.COMPANY);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy-MM-dd HH:mm:ss";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".px4 > ul > li", ".c_zw", ".limain > a", ".limain > a > p.content",
            "", "#pubtime_baidu");
    }

    @Override
    protected int getSourceId() {
        return 944;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        for (Element row : list) {
            Article article = new Article();

            CSSQuery cssQuery = this.getCSSQuery();
            this.checkDateTextExistence(row, cssQuery.getTitle());
            Elements elements = row.select(cssQuery.getTitle());
            if (elements.size() == 0) {
                logger.error("Unable to find the title element.");
                continue;
            }
            String title = HtmlParser.text(elements.first(), ".title");
            article.setTitle(title);
            article.setUrl(elements.first().attr("href"));
            this.parseSummary(row, article);

            articles.add(article);
        }
        return articles;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readDateContent(driver, article);
    }

    @Override
    protected String cleanHtml(Element dom) {
        Elements elements = dom.select("p:contains(责编：), script");
        if (elements.size() > 0) {
            elements.remove();
        }
        return super.cleanHtml(dom);
    }
}
