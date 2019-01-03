package cn.btimes.source;

import cn.btimes.model.Article;
import cn.btimes.model.BTExceptions.PastDateException;
import cn.btimes.model.Category;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.amzass.utils.common.RegexUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;

import java.text.ParseException;
import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-03 6:38 AM
 */
public class PinChain extends Source {
    private static final int MAX_PAST_DAYS = 0;
    private static final String DATE_REGEX = "\\d{4}-\\d{2}-\\d{2}";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.pinchain.com/article/tag/译讯", Category.TRAVEL);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = doc.select("article.excerpt");
        for (Element row : list) {
            Article article = new Article();
            Element linkElm = row.select("h2 > a").get(0);
            article.setUrl(linkElm.attr("href"));
            article.setTitle(linkElm.text());
            article.setSummary(HtmlParser.text(doc, "p.note"));
            articles.add(article);
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

        String timeText = HtmlParser.text(doc, "time.muted");
        article.setDate(this.parseDateText(timeText));

        Element contentElm = doc.select("article.article-content").first();
        article.setContent(this.cleanHtml(contentElm));
        this.fetchContentImages(article, contentElm);
    }

    @Override
    protected Date parseDateText(String timeText) {
        return this.parseDateTextWithDay(timeText, DATE_REGEX, DATE_FORMAT, MAX_PAST_DAYS);
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
        return 145;
    }

    @Override
    String cleanHtml(Element dom) {
        dom.select("p:contains(转载请注明：)").remove();
        String content = super.cleanHtml(dom);
        content = StringUtils.remove(content, "【品橙旅游】");
        content = StringUtils.remove(content, "品橙旅游");
        return content;
    }
}
