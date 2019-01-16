package cn.btimes.source;

import cn.btimes.model.Article;
import cn.btimes.model.BTExceptions.PastDateException;
import cn.btimes.model.Category;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.PageUtils;
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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-04 7:10 AM
 */
public class JieMian extends ThePaper {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.jiemian.com/lists/63.html", Category.ENTERTAINMENT);
        URLS.put("https://www.jiemian.com/lists/42.html", Category.LIFESTYLE);
        URLS.put("https://www.jiemian.com/lists/2.html", Category.COMPANY);
        URLS.put("https://www.jiemian.com/lists/82.html", Category.SPORTS);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = doc.select(".news-list > .list-view > .news-view");
        for (Element row : list) {
            try {
                Article article = new Article();
                String timeText = HtmlParser.text(row, ".date");
                article.setDate(this.parseDateText(timeText));

                Element linkElm = row.select("h3 > a").get(0);
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
    protected void readArticle(WebDriver driver, Article article) {
        driver.get(article.getUrl());
        WaitTime.Normal.execute();
        PageUtils.scrollToBottom(driver);
        Document doc = Jsoup.parse(driver.getPageSource());

        article.setSummary(HtmlParser.text(doc, ".article-header > p"));
        article.setSource(this.parseSource(doc));

        Element contentElm = doc.select(".article-main").first();
        article.setContent(this.cleanHtml(contentElm));
        this.fetchContentImages(article, contentElm);
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
        String keyword = "来源：";
        String source = HtmlParser.text(doc, ".article-info > span:contains(" + keyword + ")");
        return StringUtils.trim(StringUtils.remove(source, keyword));
    }

    @Override
    protected String parseContent(Document doc) {
        return null;
    }

    @Override
    protected int getSourceId() {
        return 105;
    }

    @Override
    String cleanHtml(Element dom) {
        dom.select("#ad-content, p:contains(编辑：)").remove();
        return super.cleanHtml(dom);
    }
}
