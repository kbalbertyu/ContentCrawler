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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-23 10:34 AM
 */
public class WallStreetCN extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String DATE_REGEX = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}";
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://wallstreetcn.com/news/shares", Category.FINANCE);
        URLS.put("https://wallstreetcn.com/news/bonds", Category.FINANCE);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        String cssQuery = ".article-list .list-item";
        this.checkArticleListExistence(doc, cssQuery);
        Elements list = doc.select(cssQuery);
        for (Element row : list) {
            try {
                Article article = new Article();
                String dateTextCssQuery = "time";
                this.checkDateTextExistence(row, dateTextCssQuery);
                String timeText = HtmlParser.text(row, dateTextCssQuery);
                if ((StringUtils.contains(timeText, "小时") && !StringUtils.equals(timeText, "1小时前")) ||
                    RegexUtils.match(timeText, "\\d{4}-\\d{2}-\\d{2}")) {
                    throw new PastDateException();
                }

                String titleCssQuery = "a.title";
                this.checkTitleExistence(row, titleCssQuery);
                Element linkElm = row.select(titleCssQuery).get(0);
                article.setUrl(linkElm.attr("href"));
                article.setTitle(linkElm.text());

                String summaryCssQuery = "a.content";
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
    String cleanHtml(Element dom) {
        dom.select("p:contains(本文来自), a[href*=membership], img.wscnph").remove();
        return super.cleanHtml(dom);
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        driver.get(article.getUrl());
        WaitTime.Normal.execute();
        Document doc = Jsoup.parse(driver.getPageSource());

        String dateTextCssQuery = "header > .info > time";
        this.checkDateTextExistence(doc, dateTextCssQuery);
        String timeText = HtmlParser.text(doc, dateTextCssQuery);
        article.setDate(this.parseDateText(timeText));

        article.setSource(this.parseSource(doc));

        String cssQuery = "article .rich-text";
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
        return null;
    }

    @Override
    protected String parseContent(Document doc) {
        return null;
    }

    @Override
    protected int getSourceId() {
        return 10;
    }
}
