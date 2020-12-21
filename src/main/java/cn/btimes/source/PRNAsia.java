package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.BTExceptions.PastDateException;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import com.amzass.enums.common.Country;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.common.Constants;
import com.amzass.utils.common.DateHelper;
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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/7/14 8:45
 */
public class PRNAsia extends Source {
    private static final String START_DATE = "2020-07-01";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.prnasia.com/m/mediafeed/rss?id=2721", Category.CHINESE_CONCEPT_STOCK);
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
        return new CSSQuery(".presscolumn", "#dvContent", "h3", ".abscontent",
            "", ".timestamp");
    }

    @Override
    protected int getSourceId() {
        return 396;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        int i = 0;
        for (Element row : list) {
            try {
                Article article = new Article();
                this.parseDate(row, article);
                this.parseTitle(row, article);
                String url = row.select("h3 > a").first().attr("href");
                article.setUrl(StringUtils.trim(url));

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
    void checkDate(Date date) {
        Date startDate = DateHelper.getBeginDate(START_DATE, Country.US);
        if (DateHelper.daysBetween(startDate, date) < 0) {
            throw new PastDateException();
        }
    }

    @Override
    String getStatus() {
        return "3";
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readSummaryContent(driver, article);
        this.parseRelatedArticles(driver);
    }

    private void parseRelatedArticles(WebDriver driver) {
        Document doc = Jsoup.parse(driver.getPageSource());
        Elements elements = doc.select(".storylist-block:contains(相关新闻) > .storylist-block-pre > .story-pre");
        for (Element element : elements) {
            Article article = this.parseRelatedArticle(element);
            if (article == null) continue;
            this.relatedArticles.add(article);
        }
    }

    private Article parseRelatedArticle(Element doc) {
        try {
            Article article = new Article();

            Element titleElm = doc.select("h3 > a").first();
            article.setUrl(titleElm.attr("href"));
            article.setTitle(titleElm.text());

            String timeText = HtmlParser.text(doc, ".datetime");
            article.setDate(this.parseDateText(timeText));

            return article;
        } catch (PastDateException e) {
            logger.warn("Article that past {} minutes detected, complete the list fetching: ", config.getMaxPastMinutes(), e);
            return null;
        }
    }

    @Override
    void removeDomNotInContentArea(Document doc) {
        super.removeDomNotInContentArea(doc);
        doc.select("#page-footer, header, .offiaccount, #dvKeyword, #shareBottom, #storyList").remove();
    }

    @Override
    void checkContent(Document doc) {
    }
}
