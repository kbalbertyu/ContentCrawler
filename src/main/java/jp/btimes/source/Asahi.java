package jp.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.BTExceptions.PastDateException;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import cn.btimes.model.common.Image;
import cn.btimes.utils.Common;
import com.amzass.utils.common.Constants;
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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/4/19 22:08
 */
public class Asahi extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final int MAX_PAST_DAYS = 1;
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.asahi.com/business/list/", Category.FINANCE_JP);
        URLS.put("https://www.asahi.com/national/list/", Category.SOCIETY_JP);
        URLS.put("https://www.asahi.com/politics/list/", Category.SOCIETY_JP);
        URLS.put("http://www.asahi.com/international/list/", Category.INTL_SCIENCE_JP);
        URLS.put("http://www.asahi.com/tech_science/list/", Category.INTL_SCIENCE_JP);
        URLS.put("http://www.asahi.com/apital/medicalnews/list.html?iref=com_api_med_medicalnewstop", Category.INTL_SCIENCE_JP);
        URLS.put("http://www.asahi.com/culture/list/?iref=com_cultop_all_list", Category.GENERAL_JP);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{1,2}/\\d{1,2}";
    }

    @Override
    protected String getDateFormat() {
        return "MM/dd";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".Section > ul.List > li", ".ArticleText", "a", "",
            "", ".Time");
    }

    @Override
    protected int getSourceId() {
        return 2;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        int i = 0;
        for (Element row : list) {
            try {
                Article article = new Article();
                this.parseTitle(row, article);
                String title = StringUtils.removePattern(article.getTitle(), "\\(\\d{1,2}/\\d{1,2}\\)");
                article.setTitle(title);
                this.parseDate(row, article);
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
        Document doc = Jsoup.parse(driver.getPageSource());
        Elements imageElms = doc.select(".ImagesMod > .Image > ul.Thum img");
        if (imageElms.size() == 0) {
            return;
        }
        List<Image> images = article.getContentImages();
        for (Element imageElm : imageElms) {
            String src = imageElm.attr("src");
            src = StringUtils.replace(src, "L.", ".");
            String absSrc = Common.getAbsoluteUrl(src, article.getUrl());
            Image image = new Image(absSrc, "");
            images.add(image);
        }
    }
}
