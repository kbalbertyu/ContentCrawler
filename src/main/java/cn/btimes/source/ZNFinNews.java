package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import cn.btimes.utils.Tools;
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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/8/7 21:14
 */
public class ZNFinNews extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.znfinnews.com/", Category.ECONOMY);
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
        return new CSSQuery(".page-index-left-feeds > div", ".article-detail-body", "a", "",
            "", ".time");
    }

    @Override
    protected int getSourceId() {
        return 635;
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
            String dateTextCssQuery = this.getCSSQuery().getTime();
            this.checkDateTextExistence(row, dateTextCssQuery);
            String timeText = HtmlParser.text(row, dateTextCssQuery);
            if (!Tools.containsAny(timeText, "小时", "分", "秒", "今天", "今日")) {
                logger.warn("Skip past date article: {}", timeText);
                break;
            }

            Article article = new Article();
            this.parseTitle(row, article);

            String title = HtmlParser.text(row, "div.title");
            article.setTitle(title);

            articles.add(article);
        }
        return articles;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readContent(driver, article);
    }

    @Override
    void parseCoverImageFromContent(Document doc, Article article) {
        Element imageElm = doc.select("img.article-detail-cover").first();
        if (imageElm == null) return;
        article.setCoverImage(imageElm.attr("src"));
    }
}
