package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.BTExceptions.PastDateException;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import com.amzass.utils.common.Constants;
import com.amzass.utils.common.RegexUtils;
import org.apache.commons.lang3.StringUtils;
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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-03 7:28 AM
 */
public class GasGoo extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://auto.gasgoo.com/china-news/C-102", Category.AUTO);
        URLS.put("http://auto.gasgoo.com/china-news/C-102/2", Category.AUTO);
        URLS.put("http://auto.gasgoo.com/china-news/C-102/3", Category.AUTO);
        URLS.put("http://auto.gasgoo.com/china-news/C-102/4", Category.AUTO);
        URLS.put("http://auto.gasgoo.com/nev/C-501", Category.AUTO);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy-MM-dd HH:mm:ss";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".listLeft > .content", "#ArticleContent", "h2 > a", "dl > dd > p",
            "", ".cardTime");
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        int i = 0;
        for (Element row : list) {
            try {
                Article article = new Article();
                this.parseDate(row, article);
                this.parseTitle(row, article);
                row.select("a:contains(详细)").remove();
                this.parseSummary(row, article);

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
    protected int getSourceId() {
        return 273;
    }

    @Override
    protected String cleanHtml(Element dom) {
        // Remove icon of viewing large image
        Elements elements = dom.select("img[src*=ViewImg.gif]");
        if (elements.size() > 0) {
            elements.remove();
        }

        String html = super.cleanHtml(dom);
        List<String> list = RegexUtils.getMatchedList(html, "<\\!--[\\s\\S]*-->");
        for (String str : list) {
            html = StringUtils.trim(StringUtils.remove(html, str));
        }
        return html;
    }
}
