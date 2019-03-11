package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.BTExceptions.PastDateException;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.common.Constants;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.amzass.utils.common.RegexUtils;
import org.apache.commons.lang3.StringUtils;
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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-23 10:34 AM
 */
public class WallStreetCN extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final Map<String, Category> URLS = new HashMap<>();
    private static final String DOMAIN = "wallstreetcn.com";

    static {
        URLS.put("https://wallstreetcn.com/news/shares", Category.FINANCE);
        URLS.put("https://wallstreetcn.com/news/bonds", Category.FINANCE);
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
        return new CSSQuery(".article-list .list-item", "article .rich-text", "a.title",
            "a.content", "", "header > .info > time");
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        int i = 0;
        for (Element row : list) {
            try {
                Article article = new Article();
                String dateTextCssQuery = "time";
                this.checkDateTextExistence(row, dateTextCssQuery);
                String timeText = HtmlParser.text(row, dateTextCssQuery);
                if ((StringUtils.contains(timeText, "小时") && !StringUtils.equals(timeText, "1小时前")) ||
                    RegexUtils.match(timeText, "\\d{4}-\\d{2}-\\d{2}")) {
                    throw new PastDateException("Time past limit: " + timeText);
                }
                this.parseTitle(row, article);
                this.parseSummary(row, article);

                articles.add(article);
            } catch (PastDateException e) {
                if (i++ < Constants.MAX_REPEAT_TIMES) {
                    continue;
                }
                logger.warn("Article that past {} minutes detected, complete the list fetching: ", config.getMaxPastMinutes(), e);
                break;
            }
        }
        return articles;
    }

    @Override
    protected String cleanHtml(Element dom) {
        Elements elements = dom.select("p:contains(本文来自), a[href*=membership], img.wscnph");
        if (elements.size() > 0) {
            elements.remove();
        }
        return super.cleanHtml(dom);
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        if (!StringUtils.containsIgnoreCase(article.getUrl(), DOMAIN)) {
            throw new BusinessException(String.format("Article url is from other source: %s", article.getUrl()));
        }
        this.readDateContent(driver, article);
    }

    @Override
    protected int getSourceId() {
        return 10;
    }
}
