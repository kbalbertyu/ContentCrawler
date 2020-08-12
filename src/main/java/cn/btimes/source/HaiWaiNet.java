package cn.btimes.source;

import cn.btimes.model.common.*;
import cn.btimes.model.common.BTExceptions.ArticleNoImageException;
import cn.btimes.model.common.BTExceptions.PastDateException;
import cn.btimes.utils.PageUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.amzass.model.common.ActionLog;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Constants;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.google.inject.Inject;
import com.kber.commons.DBManager;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/8/11 23:40
 */
public class HaiWaiNet extends Source {
    private static final Map<String, Category> URLS = new HashMap<>();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Inject private DBManager dbManager;

    static {
        URLS.put("http://news.haiwainet.cn/", Category.ECONOMY);
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
        return "yyy-MM-dd HH:mm:ss";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery("#list > .list", "", "a:first-child",
            "", "", ".handle > .l");
    }

    @Override
    protected int getSourceId() {
        return 79;
    }

    @Override
    String getStatus() {
        return "3";
    }

    private String getLogKey(String id) {
        return this.getClass().getSimpleName() + "-" + id;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        PageUtils.click(driver, By.cssSelector("#topNav > a:last-child"));
        WaitTime.Normal.execute();

        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            PageUtils.scrollToBottom(driver);
            WaitTime.Short.execute();
        }

        doc = Jsoup.parse(driver.getPageSource());
        Elements list = this.readList(doc);
        List<Article> articles = new ArrayList<>();
        int i = 0;
        for (Element row : list) {
            try {
                Article article = new Article();
                this.parseDate(row, article);
                CSSQuery cssQuery = this.getCSSQuery();

                Element titleElm = row.select(cssQuery.getTitle()).first();
                if (titleElm == null) continue;

                String id = titleElm.attr("data-id");
                String logKey = this.getLogKey(id);
                ActionLog log = dbManager.readById(logKey, ActionLog.class);
                if (log != null) continue;

                HaiWaiNetArticle hna = this.fetchSourceArticle(driver, id);
                article.setTitle(hna.getTitle());
                article.setUrl(hna.getLink());

                String content = StringEscapeUtils.unescapeHtml4(hna.getBody());
                article.setContent(content);

                List<HWNGallery> galleries = hna.getGalleries();
                if (CollectionUtils.isEmpty(galleries)) {
                    Document contentElm = Jsoup.parse(content);
                    this.fetchContentImages(article, contentElm);
                } else {
                    List<Image> contentImages = article.getContentImages();
                    for (HWNGallery gallery : galleries) {
                        Image image = new Image(gallery.getUrl(), gallery.getAlt());
                        contentImages.add(image);
                    }
                    article.setContentImages(contentImages);
                    article.setHideTopImages(false);
                    dbManager.save(new ActionLog(logKey), ActionLog.class);
                }

                articles.add(article);
            } catch (ArticleNoImageException e) {
                logger.error("{}", e.getMessage());
            } catch (PastDateException e) {
                if (this.ignorePastDateException()) {
                    continue;
                }
                if (i++ < Constants.MAX_REPEAT_TIMES) {
                    continue;
                }
                logger.warn("Article that past {} minutes detected, complete the list fetching: ",
                    config.getMaxPastMinutes(), e);
                break;
            } catch (BusinessException | JSONException e) {
                logger.error("Error detected when parsing article list: ", e);
            }
        }
        return articles;
    }

    private HaiWaiNetArticle fetchSourceArticle(WebDriver driver, String id) {
        String url = String.format("http://opa.haiwainet.cn/news/detail/%s?format=jsonp", id);
        driver.get(url);
        WaitTime.Normal.execute();
        String body = Jsoup.parse(driver.getPageSource()).select("pre").html();
        body = body.substring(12, body.length() - 2);
        try {
            HaiWaiNetResult hnr = JSON.parseObject(body, HaiWaiNetResult.class);
            if (!hnr.valid()) {
                throw new BusinessException(String.format("Unable to read article: %s -> %s", id, url));
            }

            return hnr.getArticle();
        } catch (JSONException e) {
            throw new BusinessException(e);
        }
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {

    }


}
