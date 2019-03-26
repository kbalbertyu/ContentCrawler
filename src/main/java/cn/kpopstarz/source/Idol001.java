package cn.kpopstarz.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-03-11 3:54 PM
 */
public class Idol001 extends Source {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://idol001.com/star/exo.html", Category.General);
        URLS.put("https://idol001.com/star/bangtan.html", Category.General);
        URLS.put("https://idol001.com/star/got7.html", Category.General);
        URLS.put("https://idol001.com/star/bigbang.html", Category.General);
        URLS.put("https://idol001.com/star/wannaone.html", Category.General);
        URLS.put("https://idol001.com/star/ikon.html", Category.General);
        URLS.put("https://idol001.com/star/blackpink.html", Category.General);
        URLS.put("https://idol001.com/star/seventeen.html", Category.General);
        URLS.put("https://idol001.com/star/nct.html", Category.General);
        URLS.put("https://idol001.com/star/huangzhilie.html", Category.General);
        URLS.put("https://idol001.com/star/gfriend.html", Category.General);
        URLS.put("https://idol001.com/star/twice.html", Category.General);
        URLS.put("https://idol001.com/star/monstax.html", Category.General);
        URLS.put("https://idol001.com/star/piaobaojian.html", Category.General);
        URLS.put("https://idol001.com/star/redvelvet.html", Category.General);
        URLS.put("https://idol001.com/star/infinite.html", Category.General);
        URLS.put("https://idol001.com/star/tara.html", Category.General);
        URLS.put("https://idol001.com/star/highlight.html", Category.General);
        URLS.put("https://idol001.com/star/cnblue.html", Category.General);
        URLS.put("https://idol001.com/star/apink.html", Category.General);
        URLS.put("https://idol001.com/star/shinee.html", Category.General);
        URLS.put("https://idol001.com/star/iu.html", Category.General);
        URLS.put("https://idol001.com/star/zhengxiujing.html", Category.General);
        URLS.put("https://idol001.com/star/lizhongshuo.html", Category.General);
        URLS.put("https://idol001.com/star/winner.html", Category.General);
        URLS.put("https://idol001.com/star/winner.html", Category.General);
        URLS.put("https://idol001.com/star/zhengxiuyan.html", Category.General);
        URLS.put("https://idol001.com/star/sosi.html", Category.General);
        URLS.put("https://idol001.com/star/2pm.html", Category.General);
        URLS.put("https://idol001.com/star/superjunior.html", Category.General);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy-MM-dd HH:mm:ss";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery("ul.card-news-list > li", ".article-detail", "a.aMask", "",
            "", ".news-info");
    }

    @Override
    protected int getSourceId() {
        return 11;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        for (Element row : list) {
            String timeText = HtmlParser.text(row, ".news-time");
            if (!Tools.containsAny(timeText, "分钟", "小时")) {
                continue;
            }

            Article article = new Article();

            CSSQuery cssQuery = this.getCSSQuery();
            this.checkTitleExistence(row, cssQuery.getTitle());
            Element linkElm = row.select(cssQuery.getTitle()).get(0);
            if (StringUtils.isBlank(article.getUrl())) {
                article.setUrl(linkElm.attr("href"));
            }
            article.setTitle(linkElm.attr("title"));

            articles.add(article);
        }
        return articles;
    }

    @Override
    protected String cleanHtml(Element dom) {
        Elements elements = dom.select("script, style");
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
