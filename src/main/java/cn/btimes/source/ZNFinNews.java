package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import cn.btimes.utils.Common;
import com.amzass.service.sellerhunt.HtmlParser;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/8/7 21:14
 */
public class ZNFinNews extends Source {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.myzaker.com/channel/13807", Category.ECONOMY);
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
        return new CSSQuery("#contentList > .content-block", "#article", ".article-content > a", "",
            "", ".article-time");
    }

    @Override
    protected int getSourceId() {
        return 635;
    }

    @Override
    String getStatus() {
        return "3";
    }

    @Override
    String getCoverSelector() {
        return "div.pic";
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        for (Element row : list) {
            Article article = this.parseDateTitleWithTimeText(row);
            if (article == null) break;

            String title = HtmlParser.text(row, "div.title");
            article.setTitle(title);

            articles.add(article);
        }
        return articles;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readContent(driver, article);
    }

    @Override
    void parseCoverImageFromContent(Document doc, Article article) {
        Element imageElm = doc.select("img.article-detail-cover").first();
        if (imageElm == null) return;
        article.setCoverImage(imageElm.attr("src"));
    }

    @Override
    void removeDomNotInContentArea(Document doc) {
        super.removeDomNotInContentArea(doc);
        doc.select("footer").remove();
    }

    @Override
    void parseCover(Element row, Article article) {
        String coverSelector = this.getCoverSelector();
        if (StringUtils.isBlank(coverSelector)) {
            return;
        }

        Element imageElm = row.select(coverSelector).first();
        if (imageElm == null) return;
        String src = imageElm.attr("data-original");
        article.setCoverImage(Common.getAbsoluteUrl(src, driver.getCurrentUrl()));
    }

    @Override
    protected String getSourceName() {
        return "锌财经";
    }
}
