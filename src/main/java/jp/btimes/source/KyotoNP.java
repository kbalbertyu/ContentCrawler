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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/4/22 16:56
 */
public class KyotoNP extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.kyoto-np.co.jp/category/news-original/経済", Category.FINANCE_JP);
        URLS.put("https://www.kyoto-np.co.jp/category/news-original/社会", Category.SOCIETY_JP);
        URLS.put("https://www.kyoto-np.co.jp/category/news-original/政治", Category.SOCIETY_JP);
        URLS.put("https://www.kyoto-np.co.jp/category/news-original/文化・ライフ", Category.GENERAL_JP);
        URLS.put("https://www.kyoto-np.co.jp/subcategory/京都府", Category.GENERAL_JP);
        URLS.put("https://www.kyoto-np.co.jp/subcategory/滋賀県", Category.GENERAL_JP);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{1,2}:\\d{1,2}";
    }

    @Override
    protected String getDateFormat() {
        return "HH:mm";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".m-articles > article", ".article-body", "h3", "",
            "", "time");
    }

    @Override
    protected int getSourceId() {
        return 25;
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
        this.readContent(driver, article);
        this.resizeContentImage(article, new String[] {"/\\d+m/" }, new String[] {"/750m/" });
    }
}
