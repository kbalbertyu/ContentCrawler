package jp.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.BTExceptions.PastDateException;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import cn.btimes.utils.PageUtils;
import com.amzass.utils.PageLoadHelper;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Constants;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/4/22 16:30
 */
public class Kanaloco extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.kanaloco.jp/news/economy/", Category.FINANCE_JP);
        URLS.put("https://www.kanaloco.jp/news/government/", Category.SOCIETY_JP);
        URLS.put("https://www.kanaloco.jp/news/international/", Category.INTL_SCIENCE_JP);
        URLS.put("https://www.kanaloco.jp/sports/", Category.GENERAL_JP);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}年\\d{1,2}月\\d{1,2}日 \\d{1,2}:\\d{1,2}";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy'年'MM'月'dd'日' HH:mm";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".blk-article-list03 > section", ".acms-entry", ".tltle", "",
            "", ".info");
    }

    @Override
    protected int getSourceId() {
        return 24;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        int i = 0;
        for (Element row : list) {
            try {
                // Skip premium articles
                if (row.select(".lock, .adsbygoogle").size() > 0) {
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
    protected void readArticle(WebDriver driver, Article article) {
        driver.get(article.getUrl());
        if (PageLoadHelper.present(driver, By.className("lazyload"), WaitTime.Normal)) {
            PageUtils.loadLazyContent(driver);
        } else {
            PageUtils.scrollToBottom(driver);
        }
        WaitTime.Short.execute();
        Document doc = Jsoup.parse(driver.getPageSource());
        this.parseContent(doc, article);
    }

    @Override
    protected String cleanHtml(Element dom) {
        Elements elements = dom.select("#modal-photo-gallery, script");
        if (elements.size() > 0) {
            elements.remove();
        }
        return super.cleanHtml(dom);
    }
}
