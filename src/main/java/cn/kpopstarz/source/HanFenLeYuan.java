package cn.kpopstarz.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.BTExceptions.PastDateException;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import cn.btimes.source.SourceWithoutDriver;
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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/6/11 9:40
 */
public class HanFenLeYuan extends SourceWithoutDriver {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.hjzlg.com/web5/YCMS_News.asp", Category.General);
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
        return new CSSQuery("", "#size", "", "",
            "", "");
    }

    @Override
    protected int getSourceId() {
        return 0;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Date date = new Date();
        String dateText = DateFormatUtils.format(date, "yyyy年MM月dd日");
        Elements list = doc.select("a:contains(" + dateText + ")");

        for (Element row : list) {
            try {
                Article article = new Article();
                String title = StringUtils.trim(StringUtils.remove(row.text(), "[" + dateText + "]"));
                article.setTitle(title);
                article.setUrl(row.attr("href"));
                article.setDate(date);
                articles.add(article);
            } catch (PastDateException e) {
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
}
