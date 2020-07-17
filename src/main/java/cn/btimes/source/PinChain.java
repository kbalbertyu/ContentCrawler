package cn.btimes.source;

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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-03 6:38 AM
 */
public class PinChain extends SourceWithoutDriver {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final int MAX_PAST_DAYS = 0;
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.pinchain.com/article/tag/译讯", Category.TRAVEL);
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
        return new CSSQuery("article.excerpt", "article.article-content", "h2 > a", "p.note", "", "time.muted");
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        int i = 0;
        for (Element row : list) {
            Article article = new Article();
            this.parseTitle(row, article);
            this.parseSummary(row, article);
            articles.add(article);
        }
        return articles;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readDateContent(driver, article);
    }

    @Override
    protected Date parseDateText(String timeText) {
        return this.parseDateTextWithDay(timeText, this.getDateRegex(), this.getDateFormat(), MAX_PAST_DAYS);
    }

    @Override
    protected int getSourceId() {
        return 145;
    }

    @Override
    protected String cleanHtml(Element dom) {
        Elements elements = dom.select("p:contains(转载请注明：)");
        if (elements.size() > 0) {
            elements.remove();
        }
        String content = super.cleanHtml(dom);
        content = StringUtils.remove(content, "【品橙旅游】");
        content = StringUtils.remove(content, "品橙旅游");
        return content;
    }
}
