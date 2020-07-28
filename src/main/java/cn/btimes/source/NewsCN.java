package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.BTExceptions.PastDateException;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import cn.btimes.utils.Common;
import cn.btimes.utils.PageUtils;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Constants;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/7/17 5:27
 */
public class NewsCN extends Source {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final int MAX_PAST_DAYS = 2;
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.news.cn/money/jrlb.htm", Category.FINANCE);
        URLS.put("http://www.news.cn/tech/qqbb.htm", Category.TECH);
        URLS.put("http://www.news.cn/auto/jsxx.htm", Category.AUTO);
        URLS.put("http://www.xinhuanet.com/house/24xsjx.htm", Category.REALESTATE);
        URLS.put("http://www.xinhuanet.com/fortune/gd.htm", Category.ECONOMY);
        URLS.put("http://www.xinhuanet.com/fortune/", Category.ECONOMY);
        URLS.put("http://www.xinhuanet.com/house/index.htm", Category.REALESTATE);
        URLS.put("http://www.xinhuanet.com/tech/index.htm", Category.TECH);
        URLS.put("http://www.xinhuanet.com/money/index.htm", Category.FINANCE);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}-\\d{2}-\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy-MM-dd";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery("#showData0 > li", "#p-detail", "h3 > a", "",
            "", ".time");
    }

    @Override
    protected int getSourceId() {
        return 7;
    }

    @Override
    String getStatus() {
        return "3";
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        int i = 0;
        for (Element row : list) {
            if (row.hasClass("moreBtn")) {
                break;
            }
            try {
                Article article = new Article();
                this.parseTitle(row, article);
                this.parseDate(row, article);
                this.parseCoverImage(row, article);
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
    void loadMoreList(WebDriver driver) {
        By by = By.id("dataMoreBtn");
        PageUtils.scrollToElement(driver, by);
        WaitTime.Shortest.execute();
        PageUtils.scrollBy(driver, 100L);
        WaitTime.Shortest.execute();
        PageUtils.click(driver, by);
    }

    private void parseCoverImage(Element row, Article article) {
        Element imageElm = row.select("img").first();
        if (imageElm == null) return;
        String src = imageElm.attr("src");
        article.setCoverImage(Common.getAbsoluteUrl(src, driver.getCurrentUrl()));
    }

    @Override
    protected Date parseDateText(String timeText) {
        return this.parseDateTextWithDay(timeText, this.getDateRegex(), this.getDateFormat(), MAX_PAST_DAYS);
    }

    @Override
    protected String cleanHtml(Element dom) {
        Elements elements = dom.select(".tadd, .p-tags, iframe, .lb, .zan-wap");
        if (elements.size() > 0) {
            elements.remove();
        }
        return super.cleanHtml(dom);
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readContent(driver, article);
    }
}
