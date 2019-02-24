package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.BTExceptions.PastDateException;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Constants;
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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-16 5:13 PM
 */
public class EEO extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.eeo.com.cn/yule/tiyu/", Category.SPORTS);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy-MM-dd HH:mm:ss";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        String urlKeyword = DateFormatUtils.format(new Date(), "yyyy/MMdd");
        return new CSSQuery("ul#lyp_article > li > a[href*=" + urlKeyword + "]",
            ".content-article", "div > span > a", "div > p",
            "", ".xd-b-b > p > span");
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        int i = 0;
        for (Element imgLinkElm : list) {
            try {
                Article article = new Article();

                Element row = imgLinkElm.parent();
                this.parseTitle(row, article);
                this.parseSummary(row, article);

                articles.add(article);
            } catch (PastDateException e) {
                if (i++ < Constants.MAX_REPEAT_TIMES) {
                    continue;
                }
                logger.warn("Article that past {} minutes detected, complete the list fetching: ", MAX_PAST_MINUTES, e);
                break;
            }
        }
        return articles;
    }

    @Override
    String cleanHtml(Element dom) {
        Elements elements = dom.select(".xd-xd-xd-rwm, .xd_zuozheinfo");
        if (elements.size() > 0) {
            elements.remove();
        }
        return super.cleanHtml(dom);
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        driver.get(article.getUrl());
        WaitTime.Short.execute();
        Document doc = Jsoup.parse(driver.getPageSource());

        this.parseDate(doc, article);
        this.parseContent(doc, article);
    }

    @Override
    protected void parseContent(Document doc, Article article) {
        Element imageElm = doc.select(".xd-xd-xd-newsimg").first();

        String contentCSSQuery = this.getCSSQuery().getContent();
        this.checkArticleContentExistence(doc, contentCSSQuery);
        Element contentElm = doc.select(contentCSSQuery).first();
        if (imageElm != null) {
            contentElm.prepend(imageElm.outerHtml());
        }
        article.setContent(this.cleanHtml(contentElm));
        this.fetchContentImages(article, contentElm);
    }

    @Override
    protected int getSourceId() {
        return 0;
    }
}
