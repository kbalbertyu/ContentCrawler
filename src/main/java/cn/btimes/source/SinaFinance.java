package cn.btimes.source;

import cn.btimes.model.Article;
import cn.btimes.model.CSSQuery;
import cn.btimes.model.Category;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-23 4:03 PM
 */
public class SinaFinance extends Sina {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://finance.sina.com.cn/roll/index.d.html?cid=56907&page=1", Category.FINANCE);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{2}月\\d{2}日 \\d{2}:\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "MM'月'dd'日' HH:mm";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery("ul.list_009 > li", "#artibody", "a",
            ".feed-card-txt-summary", ".date-source > a.source", "span");
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        this.parseDateTitleList(articles, list);
        return articles;
    }

    @Override
    protected int getSourceId() {
        return 101;
    }
}
