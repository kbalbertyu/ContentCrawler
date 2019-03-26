package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.BTExceptions.PastDateException;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.common.RegexUtils;
import org.jsoup.nodes.Element;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-03-26 3:32 PM
 */
public class IFengTravel extends IFeng {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://travel.ifeng.com/shanklist/33-60140-", Category.TRAVEL);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}年\\d{2}月\\d{2}日 \\d{2}:\\d{2}:\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy'年'MM'月'dd'日' HH:mm:ss";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".news-stream-basic-news-list > .news_item", ".main_content-LcrEruCc", "h2 > a", "",
            ".source-2pXi2vGI > a", ".time-hm3v7ddj");
    }

    @Override
    void checkRow(Element row, Article article) {
        String dateTextCssQuery = "time.news-stream-newsStream-text";
        this.checkDateTextExistence(row, dateTextCssQuery);
        String timeText = HtmlParser.text(row, dateTextCssQuery);
        if (RegexUtils.containsRegex(timeText, "\\d{2}-\\d{2}")) {
            throw new PastDateException(timeText);
        }
    }
}
