package cn.btimes.source;

import cn.btimes.model.Article;
import cn.btimes.model.CSSQuery;
import cn.btimes.model.Category;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.PageUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-04 7:10 AM
 */
public class JieMian extends ThePaper {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.jiemian.com/lists/63.html", Category.ENTERTAINMENT);
        URLS.put("https://www.jiemian.com/lists/42.html", Category.LIFESTYLE);
        URLS.put("https://www.jiemian.com/lists/2.html", Category.COMPANY);
        URLS.put("https://www.jiemian.com/lists/82.html", Category.SPORTS);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".news-list > .list-view > .news-view", ".article-main", "h3 > a",
            ".article-header > p", ".article-source > p:contains(来源：)", ".date");
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        this.parseDateTitleList(articles, list);
        return articles;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        driver.get(article.getUrl());
        WaitTime.Normal.execute();
        PageUtils.scrollToBottom(driver);
        Document doc = Jsoup.parse(driver.getPageSource());

        this.parseSummary(doc, article);
        this.parseSource(doc, article);
        this.parseContent(doc, article);
    }

    @Override
    String removeSourceNoise(String source) {
        return StringUtils.trim(StringUtils.remove(source, "来源："));
    }

    @Override
    protected int getSourceId() {
        return 105;
    }

    @Override
    String cleanHtml(Element dom) {
        Elements elements = dom.select("#ad-content, p:contains(编辑：)");
        if (elements.size() > 0) {
            elements.remove();
        }
        return super.cleanHtml(dom);
    }
}
