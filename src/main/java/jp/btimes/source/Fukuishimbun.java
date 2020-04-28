package jp.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.BTExceptions.PastDateException;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import com.amzass.utils.common.Constants;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/4/23 9:48
 */
public class Fukuishimbun extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final int MAX_PAST_DAYS = 1;
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.fukuishimbun.co.jp/category/news-fukui/経済", Category.FINANCE_JP);
        URLS.put("https://www.fukuishimbun.co.jp/category/news-fukui/政治・行政", Category.SOCIETY_JP);
        URLS.put("https://www.fukuishimbun.co.jp/category/news-fukui/社会", Category.SOCIETY_JP);
        URLS.put("https://www.fukuishimbun.co.jp/category/news-fukui/原発", Category.INTL_SCIENCE_JP);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}年\\d{1,2}月\\d{1,2}日";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy'年'MM'月'dd'日'";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery("#article-categry-list > .article", ".article-body", ".title > a > .main-title", "",
            "", ".date");
    }

    @Override
    protected int getSourceId() {
        return 13;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        int i = 0;
        for (Element row : list) {
            try {
                if (row.select("iframe").size() > 0) {
                    continue;
                }
                Article article = new Article();
                this.parseTitle(row, article);
                this.parseDate(row, article);
                article.setUrl(row.select("a").first().attr("href"));
                articles.add(article);
            } catch (PastDateException e) {
                if (i++ < Constants.MAX_REPEAT_TIMES) {
                    continue;
                }
                logger.warn("Article that past {} minutes detected, complete the list fetching: ", config.getMaxPastMinutes(), e);
                break;
            }
        }
        return articles;
    }

    @Override
    protected Date parseDateText(String timeText) {
        return this.parseDateTextWithDay(timeText, this.getDateRegex(), this.getDateFormat(), MAX_PAST_DAYS);
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readContent(driver, article);
        List<String> images = article.getContentImages();

        List<String> newImages = new ArrayList<>();
        images.forEach(image -> newImages.add(StringUtils.replace(image, "/300m/", "/750m/")));
        article.setContentImages(newImages);
    }

    @Override
    protected String cleanHtml(Element dom) {
        Elements elements = dom.select(".doublecol-rectangle, .video-ad, div[style=\"clear:both;\"], #related-article, #ob_holder, .OUTBRAIN, .admeasure, a:contains(⇒)");
        if (elements.size() > 0) {
            elements.remove();
        }
        return super.cleanHtml(dom);
    }
}
