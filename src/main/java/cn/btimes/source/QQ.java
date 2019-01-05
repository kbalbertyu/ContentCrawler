package cn.btimes.source;

import cn.btimes.model.Article;
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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-05 2:42 PM
 */
public class QQ extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String DATE_REGEX = "\\d{4}/\\d{2}/\\d{2} \\d{1,2}:\\d{1,2}";
    private static final String DATE_FORMAT = "yyyy/MM/dd HH:mm";
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://new.qq.com/omn/author/5042880", Category.ENTERTAINMENT);
        URLS.put("https://new.qq.com/rolls/?ext=sports", Category.SPORTS);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        String idPrefix = DateFormatUtils.format(new Date(), "yyyyMMdd");
        List<Article> articles = new ArrayList<>();
        Elements list = doc.select("li[id^=" + idPrefix + "]");
        for (Element row : list) {
            Article article = new Article();

            Element linkElm = row.select("h3 > a").get(0);
            article.setUrl(linkElm.attr("href"));
            String title = linkElm.text();
            if (StringUtils.contains(title, "专题") || linkElm.children().size() > 0) {
                logger.warn("Not an article link, just skip: {}", title);
                continue;
            }
            String timeText = HtmlParser.text(row, ".time");
            if (StringUtils.contains(timeText, "小时")) {
                continue;
            }
            article.setTitle(title);
            articles.add(article);
        }
        return articles;
    }

    @Override
    protected Boolean validateLink(String href) {
        return null;
    }

    @Override
    String cleanHtml(Element dom) {
        dom.select(".content-article .videoPlayer, .content-article .videoPlayerWrap, script, .video-title, .videoList, #Status").remove();
        return super.cleanHtml(dom);
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        driver.get(article.getUrl());
        WaitTime.Normal.execute();
        Document doc = Jsoup.parse(driver.getPageSource());
        String timeText = HtmlParser.text(doc, ".left-stick-wp > .year") + "/" +
            HtmlParser.text(doc, ".left-stick-wp > .md") + " " +
            HtmlParser.text(doc, ".left-stick-wp > .time");
        article.setDate(this.parseDateText(timeText));

        Element contentElm = doc.select(".content-article").first();
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
        return 809;
    }
}
