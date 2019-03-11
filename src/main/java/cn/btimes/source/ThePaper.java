package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.BTExceptions.PastDateException;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import com.amzass.service.sellerhunt.HtmlParser;
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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 12/25/2018 1:07 AM
 */
public class ThePaper extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.thepaper.cn/list_25434", Category.COMPANY);
        URLS.put("https://www.thepaper.cn/list_27234", Category.TECH);
        URLS.put("https://www.thepaper.cn/list_25433", Category.REALESTATE);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return null;
    }

    @Override
    protected String getDateFormat() {
        return null;
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".newsbox .news_li", ".news_txt", "h2 > a", "p",
            ".news_about > p > span:contains(来源)", ".pdtt_trbs > span");
    }

    @Override
    protected List<Article> parseList(Document doc) {
        int i = 0;
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        for (Element row : list) {
            try {
                Article article = new Article();
                if (StringUtils.isBlank(row.html())) {
                    continue;
                }

                try {
                    this.parseDate(doc, article);
                } catch (PastDateException e) {
                    if (i++ < Constants.MAX_REPEAT_TIMES) {
                        continue;
                    }
                    throw e;
                }

                super.parseTitle(row, article);
                this.parseSummary(row, article);
                articles.add(article);
            } catch (PastDateException e) {
                logger.warn("Article that past {} minutes detected, complete the list fetching: ", config.getMaxPastMinutes(), e);
                break;
            }
        }
        return articles;
    }

    protected Date parseDateText(String timeText) {
        return this.parseDescribableDateText(timeText);
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readTitleSourceContent(driver, article);
    }

    @Override
    public void parseTitle(Element doc, Article article) {
        String cssQuery = "h1.news_title";
        this.checkTitleExistence(doc, cssQuery);
        article.setTitle(HtmlParser.text(doc, cssQuery));
    }

    @Override
    String removeSourceNoise(String source) {
        return StringUtils.trim(StringUtils.remove(source, "来源："));
    }

    @Override
    protected int getSourceId() {
        return 12;
    }
}
