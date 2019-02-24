package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.BTExceptions.PastDateException;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Constants;
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
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.ladymax.cn/", Category.LIFESTYLE);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}年\\d{2}月\\d{2}日 \\d{2}:\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy年MM月dd日 HH:mm";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery("#list > div.i", ".newsview > .content", "a", "", "", ".newsview > .info");
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        int i = 0;
        for (Element row : list) {
            try {
                Article article = new Article();
                super.parseTitle(row, article);
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
        for (Node node : dom.childNodes()) {
            if (StringUtils.contains(node.outerHtml(), "文章来源")) {
                node.before(TO_DELETE_SEPARATOR);
                break;
            }
        }
        Elements elements = dom.select("script, [class^=ads]");
        if (elements.size() > 0) {
            elements.remove();
        }
        String html = super.cleanHtml(dom);
        return StringUtils.removePattern(html, TO_DELETE_SEPARATOR + "[\\s\\S]*");
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        driver.get(article.getUrl());
        WaitTime.Normal.execute();
        Document doc = Jsoup.parse(driver.getPageSource());

        this.parseDate(doc, article);
        this.parseTitle(doc, article);
        this.parseContent(doc, article);
    }

    @Override
    protected void parseTitle(Element doc, Article article) {
        String titleCssQuery = ".newsview > .title > h1";
        this.checkTitleExistence(doc, titleCssQuery);
        String title = HtmlParser.text(doc, titleCssQuery);
        article.setTitle(StringUtils.substringBefore(title, "|"));
    }

    @Override
    protected int getSourceId() {
        return 933;
    }
}
