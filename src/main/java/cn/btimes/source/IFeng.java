package cn.btimes.source;

import cn.btimes.model.Article;
import cn.btimes.model.BTExceptions.PastDateException;
import cn.btimes.model.CSSQuery;
import cn.btimes.model.Category;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.RegexUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-22 9:17 PM
 */
public class IFeng extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://house.ifeng.com/news/market/", Category.REALESTATE);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}年\\d{2}月\\d{2}日 \\d{2}:\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy'年'MM'月'dd'日' HH:mm";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".ni_list > a", "div.article", "dd.tt", "dd.height-50",
            ".title > .pr > span:contains(来源：)", ".title > .marb-5 > span");
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        for (Element row : list) {
            try {
                Article article = new Article();
                String dateTextCssQuery = ".grey > span.fl";
                this.checkDateTextExistence(row, dateTextCssQuery);
                String timeText = HtmlParser.text(row, dateTextCssQuery);
                if (RegexUtils.match(timeText, "\\d{4}-\\d{2}-\\d{2}")) {
                    continue;
                }

                article.setUrl(row.attr("href"));
                this.parseTitle(row, article);
                this.parseSummary(row, article);

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
        driver.get(article.getUrl());
        WaitTime.Normal.execute();
        Document doc = Jsoup.parse(driver.getPageSource());

        this.parseDate(doc, article);
        this.parseSource(doc, article);
        this.parseContent(doc, article);
    }

    @Override
    String removeSourceNoise(String source) {
        return StringUtils.substringAfter(source, "来源：");
    }

    @Override
    protected int getSourceId() {
        return 706;
    }
}