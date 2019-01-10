package cn.btimes.source;

import cn.btimes.model.Article;
import cn.btimes.model.BTExceptions.PastDateException;
import cn.btimes.model.Category;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Tools;
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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-06 12:05 AM
 */
public class CTO51 extends Source {
    private static final String DATE_REGEX = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String TO_DELETE_SEPARATOR = "###TO-DELETE###";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://iot.51cto.com/", Category.FUTURE_INDUSTRIES);
        URLS.put("http://ai.51cto.com/", Category.FUTURE_INDUSTRIES);
        URLS.put("http://bigdata.51cto.com/", Category.FUTURE_INDUSTRIES);
        URLS.put("http://cloud.51cto.com/", Category.FUTURE_INDUSTRIES);
        URLS.put("http://blockchain.51cto.com/", Category.FUTURE_INDUSTRIES);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = doc.select(".home-left-list > ul > li");
        for (Element row : list) {
            try {
                if (Tools.contains(row.attr("class"), "adv")) {
                    continue;
                }
                Article article = new Article();
                String timeText = HtmlParser.text(row, ".time > i");
                article.setDate(this.parseDateText(timeText));

                Element linkElm = row.select(".rinfo > a").get(0);
                article.setUrl(linkElm.attr("href"));
                article.setTitle(linkElm.text());

                article.setSummary(HtmlParser.text(row, ".rinfo > p"));

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
            if (Tools.containsAny(node.outerHtml(), "责任编辑：", "编辑推荐")) {
                node.before(TO_DELETE_SEPARATOR);
                break;
            }
        }
        String html = super.cleanHtml(dom);
        return StringUtils.removePattern(html, TO_DELETE_SEPARATOR + "[\\s\\S]*");
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        driver.get(article.getUrl());
        WaitTime.Normal.execute();
        Document doc = Jsoup.parse(driver.getPageSource());

        article.setSource(this.parseSource(doc));

        Element contentElm = doc.select(".zwnr").first();
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
        String source = HtmlParser.text(doc, "dl > dt > span:contains(来源：)");
        return StringUtils.trim(StringUtils.substringAfter(source, "来源："));
    }

    @Override
    protected String parseContent(Document doc) {
        return null;
    }

    @Override
    protected int getSourceId() {
        return 0;
    }
}
