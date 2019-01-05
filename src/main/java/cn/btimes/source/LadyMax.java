package cn.btimes.source;

import cn.btimes.model.Article;
import cn.btimes.model.BTExceptions.PastDateException;
import cn.btimes.model.Category;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.PageLoadHelper.WaitTime;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-05 4:41 PM
 */
public class LadyMax extends Source {
    private static final String TO_DELETE_SEPARATOR = "###TO-DELETE###";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String DATE_REGEX = "\\d{4}年\\d{2}月\\d{2}日 \\d{2}:\\d{2}";
    private static final String DATE_FORMAT = "yyyy年MM月dd日 HH:mm";
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.ladymax.cn/", Category.LIFESTYLE);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = doc.select("#list > div.i");
        for (Element row : list) {
            try {
                Article article = new Article();
                Element titleElm = row.select("a").get(1);
                article.setUrl(titleElm.attr("href"));
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
        for (Node node : dom.childNodes()) {
            if (StringUtils.contains(node.outerHtml(), "文章来源")) {
                node.before(TO_DELETE_SEPARATOR);
                break;
            }
        }
        dom.select("script, [class^=ads]").remove();
        String html = super.cleanHtml(dom);
        return StringUtils.removePattern(html, TO_DELETE_SEPARATOR + "[\\s\\S]*");
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        driver.get(article.getUrl());
        WaitTime.Normal.execute();
        Document doc = Jsoup.parse(driver.getPageSource());

        String timeText = HtmlParser.text(doc, ".newsview > .info");
        article.setDate(this.parseDateText(timeText));

        String title = HtmlParser.text(doc, ".newsview > .title > h1");
        article.setTitle(StringUtils.substringBefore(title, "|"));

        Element contentElm = doc.select(".newsview > .content").first();
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
        return 933;
    }
}
