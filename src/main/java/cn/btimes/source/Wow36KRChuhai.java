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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/9/11 16:30
 */
public class Wow36KRChuhai extends Source {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://weixin.sogou.com/weixin?type=1&s_from=input&query=wow36krchuhai&ie=utf8&_sug_=n&_sug_type_=", Category.ECONOMY);
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
        return new CSSQuery("[uigs=account_article_0]", "#js_content", "a",
            "", "", "");
    }

    @Override
    protected int getSourceId() {
        return 1874;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        this.parseTitleList(articles, list);
        return articles;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readContent(driver, article);
        article.setUrl(driver.getCurrentUrl());
    }

    @Override
    String getContentImageSrcAttr() {
        return "data-src";
    }

    @Override
    protected String cleanHtml(Element dom) {
        Element nextElm = dom.select("section:contains(联系36氪出海)").first();
        if (nextElm != null) {
            ArrayList<Element> toDeleteElms = new ArrayList<>();
            toDeleteElms.add(nextElm);
            while (true) {
                nextElm = nextElm.nextElementSibling();
                if (nextElm == null) {
                    break;
                }
                toDeleteElms.add(nextElm);
            }
            for (Element element : toDeleteElms) {
                element.remove();
            }
        }
        return super.cleanHtml(dom);
    }
}
