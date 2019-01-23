package cn.btimes.source;

import cn.btimes.model.Article;
import cn.btimes.model.BTExceptions.PastDateException;
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
    private static final String DATE_REGEX = "\\d{4}年\\d{2}月\\d{2}日 \\d{2}:\\d{2}";
    private static final String DATE_FORMAT = "yyyy'年'MM'月'dd'日' HH:mm";
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
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        String cssQuery = ".ni_list > a";
        this.checkArticleListExistence(doc, cssQuery);
        Elements list = doc.select(cssQuery);
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
                String titleCssQuery = "dd.tt";
                this.checkTitleExistence(row, titleCssQuery);
                Element linkElm = row.select(titleCssQuery).get(0);
                article.setTitle(linkElm.text());

                String summaryCssQuery = "dd.height-50";
                this.checkSummaryExistence(row, summaryCssQuery);
                article.setSummary(HtmlParser.text(row, summaryCssQuery));

                articles.add(article);
            } catch (PastDateException e) {
                logger.warn("Article that past {} minutes detected, complete the list fetching.", MAX_PAST_MINUTES);
                break;
            }
        }
        return articles;
    }

    @Override
    protected Boolean validateLink(String href) {
        return null;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        driver.get(article.getUrl());
        WaitTime.Normal.execute();
        Document doc = Jsoup.parse(driver.getPageSource());

        String dateTextCssQuery = ".title > .marb-5 > span";
        this.checkDateTextExistence(doc, dateTextCssQuery);
        String timeText = HtmlParser.text(doc, dateTextCssQuery);
        article.setDate(this.parseDateText(timeText));

        article.setSource(this.parseSource(doc));

        String cssQuery = "div.article";
        this.checkArticleContentExistence(doc, cssQuery);
        Element contentElm = doc.select(cssQuery).first();
        article.setContent(this.cleanHtml(contentElm));
        this.fetchContentImages(article, contentElm);
    }

    @Override
    protected Date parseDateText(String timeText) {
        return this.parseDateText(timeText, DATE_REGEX, DATE_FORMAT);
    }

    @Override
    protected Date parseDate(Document doc) {
        return null;
    }

    @Override
    protected void validateDate(Date date) {

    }

    @Override
    protected String parseTitle(Document doc) {
        return null;
    }

    @Override
    protected String parseSource(Document doc) {
        String source = HtmlParser.text(doc, ".title > .pr > span:contains(来源：)");
        return StringUtils.substringAfter(source, "来源：");
    }

    @Override
    protected String parseContent(Document doc) {
        return null;
    }

    @Override
    protected int getSourceId() {
        return 706;
    }
}
