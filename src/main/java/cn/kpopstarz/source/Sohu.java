package cn.kpopstarz.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import cn.btimes.source.SourceWithoutDriver;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.common.Tools;
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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-03-11 4:38 PM
 */
public class Sohu extends SourceWithoutDriver {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://mp.sohu.com/profile?xpt=aGFueXVwaW5kYW9Ac29odS5jb20=&spm=smpc.ch19.fd.348.1551954500340cuGyciV", Category.General);
        URLS.put("https://mp.sohu.com/profile?xpt=aGFubnZ0dWFuQHNvaHUuY29t&_f=index_pagemp_1&spm=smpc.content.author.1.1551953583936m63EZyx", Category.General);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy-MM-dd HH:mm";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery("ul.feed-list-area > li", "article.article", "h4 > a", "", "", "#news-time");
    }

    @Override
    protected int getSourceId() {
        return 12;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        for (Element row : list) {
            if (!StringUtils.startsWith(row.attr("data-spm-content"), "a/")) {
                continue;
            }
            String timeText = HtmlParser.text(row, ".time");
            if (!Tools.containsAny(timeText, "分钟", "小时")) {
                continue;
            }

            Article article = new Article();
            this.parseTitle(row, article);
            articles.add(article);
        }
        return articles;
    }

    @Override
    protected String cleanHtml(Element dom) {
        Elements elements = dom.select("#backsohucom, .backword, .editor-name, [data-role=original-title]");
        if (elements.size() > 0) {
            elements.remove();
        }
        return super.cleanHtml(dom);
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readDateContent(driver, article);
    }
}
