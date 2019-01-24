package cn.btimes.source;

import cn.btimes.model.Article;
import cn.btimes.model.CSSQuery;
import cn.btimes.model.Category;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.PageUtils;
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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-03 6:03 AM
 */
public class LvJie extends ThePaper {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.lvjie.com.cn/destination/", Category.TRAVEL);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery("#data_list > .li", ".detailCont", ".Tlist > a", "p.listCont",
            ".Wh > .baodao", ".time");
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        this.parseDateTitleSummaryList(articles, list);
        return articles;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        driver.get(article.getUrl());
        WaitTime.Normal.execute();
        PageUtils.scrollToBottom(driver);
        Document doc = Jsoup.parse(driver.getPageSource());

        this.parseSource(doc, article);
        this.parseContent(doc, article);
    }

    @Override
    protected int getSourceId() {
        return 939;
    }

    @Override
    String cleanHtml(Element dom) {
        Elements elements = dom.select("[class*=subject], [style*=italic], img[src*=static]");
        if (elements.size() > 0) {
            elements.remove();
        }
        return super.cleanHtml(dom);
    }
}
