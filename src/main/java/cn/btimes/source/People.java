package cn.btimes.source;

import cn.btimes.model.Article;
import cn.btimes.model.BTExceptions.PastDateException;
import cn.btimes.model.Category;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.PageLoadHelper.WaitTime;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-22 8:36 PM
 */
public class People extends Source {
    private static final String DATE_REGEX = "\\d{4}年\\d{2}月\\d{2}日\\d{2}:\\d{2}";
    private static final String DATE_FORMAT = "yyyy'年'MM'月'dd'日'HH:mm";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://travel.people.com.cn/GB/41636/41642/index.html", Category.TRAVEL);
        URLS.put("http://travel.people.com.cn/GB/41636/41641/index.html", Category.TRAVEL);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        String cssQuery = ".ej_list_box > ul > li";
        this.checkArticleListExistence(doc, cssQuery);
        Elements list = doc.select(cssQuery);
        for (Element row : list) {
            try {
                Article article = new Article();
                String dateTextCssQuery = "em";
                this.checkDateTextExistence(row, dateTextCssQuery);
                String timeText = HtmlParser.text(row, dateTextCssQuery);
                String today = DateFormatUtils.format(new Date(), "yyyy-MM-dd");
                if (!StringUtils.equals(timeText, today)) {
                    break;
                }

                String titleCssQuery = "a";
                this.checkTitleExistence(row, titleCssQuery);
                Element linkElm = row.select(titleCssQuery).get(0);
                article.setUrl(linkElm.attr("href"));
                article.setTitle(linkElm.text());

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
        dom.select(".edit").remove();
        return super.cleanHtml(dom);
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        driver.get(article.getUrl());
        WaitTime.Normal.execute();
        Document doc = Jsoup.parse(driver.getPageSource());

        String dateTextCssQuery = ".box01 > .fl";
        this.checkDateTextExistence(doc, dateTextCssQuery);
        String timeText = HtmlParser.text(doc, dateTextCssQuery);
        article.setDate(this.parseDateText(timeText));

        String cssQuery = "#rwb_zw";
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
        return 33;
    }
}
