package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-05 4:41 PM
 */
public class LadyMax extends SourceWithoutDriver {
    private static final String TO_DELETE_SEPARATOR = "###TO-DELETE###";
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.ladymax.cn/", Category.LIFESTYLE);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}年\\d{2}月\\d{2}日 \\d{2}:\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy年MM月dd日 HH:mm";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery("#list > div.i", ".newsview > .content", "a.tt", "", "", ".newsview > .info");
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        for (Element row : list) {
            Article article = new Article();
            this.parseTitle(row, article);
            articles.add(article);
        }
        return articles;
    }

    @Override
    protected String cleanHtml(Element dom) {
        for (Node node : dom.childNodes()) {
            if (StringUtils.contains(node.outerHtml(), "文章来源")) {
                node.before(TO_DELETE_SEPARATOR);
                break;
            }
        }
        Elements elements = dom.select("script, [class^=ads]");
        if (elements.size() > 0) {
            elements.remove();
        }
        String html = super.cleanHtml(dom);
        return StringUtils.removePattern(html, TO_DELETE_SEPARATOR + "[\\s\\S]*");
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readDateContent(driver, article);
    }

    @Override
    public void parseTitle(Element doc, Article article) {
        String titleCssQuery = this.getCSSQuery().getTitle();
        this.checkTitleExistence(doc, titleCssQuery);
        Element element = doc.select(titleCssQuery).get(0);
        element.select("i").remove();
        article.setUrl(element.attr("href"));
        article.setTitle(StringUtils.substringAfter(element.text(), "|").trim());
    }

    @Override
    protected int getSourceId() {
        return 933;
    }
}
