package cn.btimes.source;

import cn.btimes.model.Article;
import cn.btimes.model.BTExceptions.PastDateException;
import cn.btimes.model.CSSQuery;
import cn.btimes.model.Category;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-03 4:38 AM
 */
public class HeXun extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://house.hexun.com/list/", Category.REALESTATE);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{2}/\\d{2} \\d{2}:\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "MM/dd HH:mm";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery("#temp01 > ul > li", ".art_contextBox", "a", "",
            "span", "");
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        for (Element row : list) {
            try {
                if (StringUtils.isBlank(row.text())) {
                    continue;
                }
                Article article = new Article();
                this.parseDate(row, article);
                this.parseTitle(row, article);

                articles.add(article);
            } catch (PastDateException e) {
                logger.warn("Article that past {} minutes detected, complete the list fetching.", MAX_PAST_MINUTES);
                break;
            }
        }
        return articles;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        driver.get(article.getUrl());
        WaitTime.Normal.execute();
        PageUtils.scrollToBottom(driver);
        Document doc = Jsoup.parse(driver.getPageSource());
        this.parseContent(doc, article);
    }

    @Override
    protected int getSourceId() {
        return 842;
    }

    @Override
    String cleanHtml(Element dom) {
        dom.select(":contains(责任编辑：), a:contains(下载APP)").remove();
        return super.cleanHtml(dom);
    }
}
