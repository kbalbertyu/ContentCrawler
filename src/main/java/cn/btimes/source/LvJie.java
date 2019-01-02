package cn.btimes.source;

import cn.btimes.model.Article;
import cn.btimes.model.BTExceptions.PastDateException;
import cn.btimes.model.Category;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.PageUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-03 6:03 AM
 */
public class LvJie extends ThePaper {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.lvjie.com.cn/destination/", Category.TRAVEL);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = doc.select("#data_list > .li");
        for (Element row : list) {
            try {
                Article article = new Article();
                String timeText = HtmlParser.text(row, ".time");
                article.setDate((this.parseDateText(timeText)));

                Element linkElm = row.select(".Tlist > a").get(0);
                article.setUrl(linkElm.attr("href"));
                article.setTitle(linkElm.text());

                article.setSummary(HtmlParser.text(row, "p.listCont"));
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

        article.setSource(this.parseSource(doc));

        Element contentElm = doc.select(".detailCont").first();
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
        return HtmlParser.text(doc, ".Wh > .baodao");
    }

    @Override
    protected String parseContent(Document doc) {
        return null;
    }

    @Override
    protected int getSourceId() {
        return 939;
    }

    @Override
    String cleanHtml(Element dom) {
        dom.select("[class*=subject], [style*=italic], img[src*=static]").remove();
        return super.cleanHtml(dom);
    }
}
