package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.BTExceptions.PastDateException;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.common.Constants;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-22 8:36 PM
 */
public class People extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://travel.people.com.cn/GB/41636/41642/index.html", Category.TRAVEL);
        URLS.put("http://travel.people.com.cn/GB/41636/41641/index.html", Category.TRAVEL);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}年\\d{2}月\\d{2}日\\d{2}:\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy'年'MM'月'dd'日'HH:mm";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".ej_list_box > ul > li", "#rwb_zw", "a", "",
            "", ".box01 > .fl");
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        int i = 0;
        for (Element row : list) {
            try {
                Article article = new Article();
                String dateTextCssQuery = "em";
                this.checkDateTextExistence(row, dateTextCssQuery);
                String timeText = HtmlParser.text(row, dateTextCssQuery);
                String today = DateFormatUtils.format(new Date(), "yyyy-MM-dd");
                if (!StringUtils.equals(timeText, today)) {
                    break;
                }

                this.parseTitle(row, article);
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
        Elements elements = dom.select(".edit");
        if (elements.size() > 0) {
            elements.remove();
        }
        return super.cleanHtml(dom);
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readDateContent(driver, article);
    }

    @Override
    protected int getSourceId() {
        return 33;
    }
}
