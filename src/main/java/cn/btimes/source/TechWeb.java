package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/8/21 12:00
 */
public class TechWeb extends SourceWithoutDriver {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.techweb.com.cn/it/", Category.TECH);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}\\.\\d{2}\\.\\d{2} \\d{2}:\\d{2}:\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy.MM.dd HH:mm:ss";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".list_con > .picture_text", "#content", "> .text > a:first-child", "> .text > p",
            "", ".infos > .time");
    }

    @Override
    protected int getSourceId() {
        return 62;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        this.parseTitleSummaryList(articles, list);
        return articles;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readDateContent(driver, article);
    }
}
