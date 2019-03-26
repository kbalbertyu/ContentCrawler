package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.BTExceptions.PastDateException;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import com.amzass.utils.common.Constants;
import com.amzass.utils.common.Tools;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-06 12:05 AM
 */
public class CTO51 extends Source {
    private static final String TO_DELETE_SEPARATOR = "###TO-DELETE###";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://ai.51cto.com/", Category.FUTURE_INDUSTRIES);
        URLS.put("http://bigdata.51cto.com/", Category.FUTURE_INDUSTRIES);
        URLS.put("http://cloud.51cto.com/", Category.FUTURE_INDUSTRIES);
        URLS.put("http://blockchain.51cto.com/", Category.FUTURE_INDUSTRIES);
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
        return new CSSQuery(".home-left-list > ul > li", ".zwnr", ".rinfo > a",
            ".rinfo > p", "dl > dt > span:contains(来源：)", ".time > i");
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        int i = 0;
        for (Element row : list) {
            try {
                if (Tools.contains(row.attr("class"), "adv")) {
                    continue;
                }
                Article article = new Article();
                this.parseDate(row, article);
                this.parseTitle(row, article);
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
    protected String cleanHtml(Element dom) {
        for (Node node : dom.childNodes()) {
            if (Tools.containsAny(node.outerHtml(), "责任编辑：", "编辑推荐")) {
                node.before(TO_DELETE_SEPARATOR);
                break;
            }
        }
        String html = super.cleanHtml(dom);
        return StringUtils.removePattern(html, TO_DELETE_SEPARATOR + "[\\s\\S]*");
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readContentSource(driver, article);
    }

    @Override
    String removeSourceNoise(String source) {
        return StringUtils.trim(StringUtils.substringAfter(source, "来源："));
    }

    @Override
    protected int getSourceId() {
        return 0;
    }
}
