package cn.kpopstarz.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.BTExceptions.PastDateException;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import cn.btimes.source.SourceWithoutDriver;
import com.amzass.utils.common.Constants;
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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/12/11 1:36
 */
public class BNTNews extends SourceWithoutDriver {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.bntnews.cn/app/news_list.php?mg=4&sg=99&page=1", Category.General);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy-MM-dd HH:mm";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".arti_ttl1.sp01", "#__newsBody__ > p.arti_txt6", "#__newsBody__ > p.arti_ttl6", "",
            "", "");
    }

    @Override
    protected int getSourceId() {
        return 21;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);

        int i = 0;
        for (Element row : list) {
            try {
                Article article = new Article();
                article.setDate(this.parseDateText(row.parent().text()));

                Element linkElm = row.select("a").get(0);
                article.setUrl(linkElm.attr("href"));

                articles.add(article);
            } catch (PastDateException e) {
                if (i++ < Constants.MAX_REPEAT_TIMES) {
                    continue;
                }
                logger.warn("Article that past {} minutes detected, complete the list fetching: ",
                    config.getMaxPastMinutes(), e);
                break;
            }
        }
        return articles;
    }

    @Override
    protected String cleanHtml(Element dom) {
        Elements elements = dom.select("a[href^=/app/news.php], a[href^=mailto]");
        if (elements.size() > 0) {
            elements.remove();
        }
        return StringUtils.remove(super.cleanHtml(dom), "bnt新闻 投稿邮箱");
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readTitleContent(driver, article);
    }
}
