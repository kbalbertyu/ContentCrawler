package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import cn.btimes.model.common.Image;
import cn.btimes.utils.Common;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.PageLoadHelper;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.RegexUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/7/30 10:59
 */
public class ChinaNetPhoto extends Source {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.xinhuanet.com/photo/zxtp.htm", Category.GALLERY);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}-\\d{1,2}-\\d{1,2} \\d{2}:\\d{2}:\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy-MM-dd HH:mm:ss";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".product_list", "", "h3 > a",
            "", "", ".time");
    }

    @Override
    protected int getSourceId() {
        return 7;
    }

    @Override
    String getStatus() {
        return "3";
    }

    @Override
    String getTypology() {
        return "2";
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        this.parseDateTitleList(articles, list);
        return articles;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        Document doc = this.openArticlePage(driver, article);
        Map<Integer, String> pageLinks = new HashMap<>();
        String currentUrl = driver.getCurrentUrl();
        pageLinks.put(1, currentUrl);
        this.parsePageLinks(doc, pageLinks, currentUrl);

        List<Image> images = this.crawlGalleryImages(driver, pageLinks);
        article.setContentImages(images);
    }

    private List<Image> crawlGalleryImages(WebDriver driver, Map<Integer, String> pageLinks) {
        List<Image> images = new ArrayList<>();
        for (int page = 1; page <= pageLinks.size(); page++) {
            String pageLink = pageLinks.get(page);
            driver.get(pageLink);
            if (!PageLoadHelper.present(driver, By.cssSelector("#content img"), WaitTime.Normal)) {
                continue;
            }

            Document doc = Jsoup.parse(driver.getPageSource());
            Image image = this.parseGalleryImage(doc, driver.getCurrentUrl());
            if (image != null && StringUtils.isNotBlank(image.getUrl())) {
                images.add(image);
            }
        }
        return images;
    }

    private Image parseGalleryImage(Document doc, String currentUrl) {
        Element imageElm = doc.select(".dask > img").first();
        if (imageElm == null) {
            return null;
        }
        String src = Common.getAbsoluteUrl(imageElm.attr("src"), currentUrl);

        String content = StringUtils.trim(HtmlParser.text(doc, "#content > p:last-child"));
        content = Common.trim(content);
        if (StringUtils.isBlank(content)) {
            content = StringUtils.EMPTY;
        }
        return new Image(src, content);
    }

    private void parsePageLinks(Document doc, Map<Integer, String> pageLinks, String currentUrl) {
        Element currentPageElm = doc.select("#div_currpage").first();

        // Only one page
        if (currentPageElm == null) return;

        Element parentElm = currentPageElm.parent();
        Elements linkElms = parentElm.select("a");
        for (Element linkElm : linkElms) {
            String text = linkElm.text().trim();
            if (!RegexUtils.match(text, "\\d+")) {
                continue;
            }
            int page = NumberUtils.toInt(text);
            if (pageLinks.containsKey(page)) {
                continue;
            }
            String absLink = Common.getAbsoluteUrl(linkElm.attr("href"), currentUrl);
            pageLinks.put(page, absLink);
        }
    }
}
