package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.BTExceptions.ArticleNoImageException;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import cn.btimes.model.common.Image;
import cn.btimes.utils.PageUtils;
import com.amzass.utils.PageLoadHelper;
import com.amzass.utils.PageLoadHelper.WaitTime;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-23 3:29 PM
 */
public class CSCOMCN extends Source {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.cs.com.cn/", Category.ECONOMY);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{2}-\\d{2}-\\d{2} \\d{2}:\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "yy-MM-dd HH:mm";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".box-bigpic, .box-pic", ".article-t", "a:last-child", "", "", ".info > p > em");
    }

    @Override
    String getCoverSelector() {
        return "img";
    }

    @Override
    String getStatus() {
        return "3";
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
        Document doc = this.openArticlePage(driver, article);

        StringBuilder sb = new StringBuilder();

        while (true) {
            if (article.getDate() == null) {
                this.parseDate(doc, article);
            }
            this.parseContent(doc, article);
            sb.append(article.getContent());

            By nextBy = By.cssSelector("a:contains(下一页)");
            if (!PageLoadHelper.clickable(driver, nextBy, WaitTime.Short)) {
                break;
            }
            PageUtils.click(driver, nextBy);
            WaitTime.Normal.execute();
        }

        if (CollectionUtils.isEmpty(article.getContentImages())) {
            String cover = article.getCoverImage();
            if (StringUtils.isNotBlank(cover)) {
                Image imageObj = new Image(cover, "");
                article.getContentImages().add(imageObj);
            } else {
                String message = String.format("This article is skipped due to having no images: %d -> %s",
                    article.getId(), article.getTitle());
                throw new ArticleNoImageException(message);
            }
        }
        String content = sb.toString();
        article.setContent(content);
    }

    @Override
    protected int getSourceId() {
        return 297;
    }

    @Override
    boolean allowArticleWithoutImage() {
        return true;
    }
}
