package cn.btimes.source;

import cn.btimes.model.Article;
import cn.btimes.model.BTExceptions.PastDateException;
import cn.btimes.model.Category;
import com.alibaba.fastjson.JSONObject;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Constants;
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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-05 8:03 PM
 */
public class IYiOu extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String DATE_REGEX = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}";
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.iyiou.com/AI/", Category.FUTURE_INDUSTRIES);
        URLS.put("https://www.iyiou.com/smartcity/", Category.FUTURE_INDUSTRIES);
        URLS.put("https://www.iyiou.com/new_manufacturing/", Category.FUTURE_INDUSTRIES);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        String cssQuery = "ul.newestArticleList > li.thinkTankTag";
        this.checkArticleListExistence(doc, cssQuery);
        Elements list = doc.select(cssQuery);
        for (Element row : list) {
            try {
                Article article = new Article();
                String timeText = HtmlParser.text(row, ".time");
                if (StringUtils.contains(timeText, "小时") || RegexUtils.match(timeText, "\\d{4}-\\d{2}-\\d{2}")) {
                    throw new PastDateException();
                }

                String titleCssQuery = "h2";
                this.checkTitleExistence(row, titleCssQuery);
                Element titleElm = row.select(titleCssQuery).get(0);
                article.setUrl(titleElm.parent().attr("href"));
                article.setTitle(titleElm.text());

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
        dom.select("#machineContainer, .copyrightState, p:contains(本文来源)").remove();
        return super.cleanHtml(dom);
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        driver.get(article.getUrl());
        WaitTime.Normal.execute();
        Document doc = Jsoup.parse(driver.getPageSource());

        String dateTextCssQuery = "script[type=application/ld+json]";
        this.checkDateTextExistence(doc, dateTextCssQuery);
        Element element = doc.select(dateTextCssQuery).get(0);
        JSONObject obj = JSONObject.parseObject(element.html());
        article.setDate(this.parseDateText(String.valueOf(obj.get("pubDate"))));

        String summaryCssQuery = "#post_brief";
        this.checkSummaryExistence(doc, summaryCssQuery);
        Element summaryElm = doc.select(summaryCssQuery).get(0);
        summaryElm.select("b").remove();
        article.setSummary(summaryElm.text());

        String cssQuery = "#post_description";
        this.checkArticleContentExistence(doc, cssQuery);
        Element contentElm = doc.select(cssQuery).first();
        Elements thumbnailELms = doc.select("#post_thumbnail");
        if (thumbnailELms.size() > 0) {
            Element thumbElm = thumbnailELms.first();
            thumbElm.select(".post_copyright").remove();
            contentElm.prepend(thumbElm.outerHtml());
        }

        this.convertImageToNoneHttps(contentElm);
        article.setContent(this.cleanHtml(contentElm));
        this.fetchContentImages(article, contentElm);
    }

    private void convertImageToNoneHttps(Element contentElm) {
        Elements images = contentElm.select("img");
        for (Element image : images) {
            String src = image.attr("src");
            if (StringUtils.startsWith(src, Constants.HTTPS)) {
                src = StringUtils.replace(src, Constants.HTTPS, Constants.HTTP);
                image.attr("src", src);
            }
        }
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
        return 1004;
    }
}
