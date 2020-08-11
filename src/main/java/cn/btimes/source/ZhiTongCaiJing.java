package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/8/11 10:57
 */
public class ZhiTongCaiJing extends Source {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.zhitongcaijing.com/content/recommend.html", Category.ECONOMY);
        URLS.put("https://www.zhitongcaijing.com/focus.html", Category.ECONOMY);
        URLS.put("https://www.zhitongcaijing.com/company.html", Category.ECONOMY);
        URLS.put("https://www.zhitongcaijing.com/shares.html", Category.ECONOMY);
        URLS.put("https://www.zhitongcaijing.com/market.html", Category.ECONOMY);
        URLS.put("https://www.zhitongcaijing.com/announcement.html", Category.ECONOMY);
        URLS.put("https://www.zhitongcaijing.com/research.html", Category.ECONOMY);
        URLS.put("https://www.zhitongcaijing.com/content/read.html", Category.ECONOMY);
        URLS.put("https://www.zhitongcaijing.com/content/meigu.html", Category.ECONOMY);
        URLS.put("https://www.zhitongcaijing.com/chance.html", Category.ECONOMY);
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
        return new CSSQuery(".list-art > dl", "article", "h2 > a", "",
            "", ".format-time");
    }


    @Override
    String getCoverSelector() {
        return "dt > a > img";
    }

    @Override
    protected int getSourceId() {
        return 984;
    }

    @Override
    String getStatus() {
        return "3";
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        for (Element row : list) {
            Article article = this.parseDateTitleWithTimeText(row);
            if (article == null) break;
            articles.add(article);
        }
        return articles;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readContent(driver, article);
    }

    @Override
    protected String cleanHtml(Element dom) {
        Elements elements = dom.select(".type, h1, .h-30, #author_external");
        if (elements.size() > 0) elements.remove();
        return super.cleanHtml(dom);
    }
}
