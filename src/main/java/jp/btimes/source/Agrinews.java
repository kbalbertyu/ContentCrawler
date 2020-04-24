package jp.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.BTExceptions.PastDateException;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import com.amzass.utils.common.Constants;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/4/22 16:54
 */
public class Agrinews extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final int MAX_PAST_DAYS = 1;
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.agrinews.co.jp/tag_index?tag_id=14", Category.FINANCE_JP);
        URLS.put("https://www.agrinews.co.jp/tag_index?tag_id=11", Category.SOCIETY_JP);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}年\\d{2}月\\d{2}日";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy'年'MM'月'dd'日'";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".articles > a", ".article_detail", ".article_title", "",
            "", ".article_date");
    }

    @Override
    protected int getSourceId() {
        return 1;
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
                this.parseDate(row, article);
                article.setUrl(row.attr("href"));
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
        this.readContent(driver, article);
    }

    @Override
    protected Date parseDateText(String timeText) {
        return this.parseDateTextWithDay(timeText, this.getDateRegex(), this.getDateFormat(), MAX_PAST_DAYS);
    }

    @Override
    protected String cleanHtml(Element dom) {
        Elements elements = dom.select("img.blankImg, font:contains(点击这里订阅日本农业报纸), div[style=\"text-align: center; padding-top: 27px; margin-top: 17px; border-top: 1px solid;\"]");
        if (elements.size() > 0) {
            elements.remove();
        }
        return super.cleanHtml(dom);
    }
}
