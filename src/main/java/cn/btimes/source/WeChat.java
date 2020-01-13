package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import cn.btimes.model.common.Config;
import cn.btimes.model.wechat.Content;
import cn.btimes.model.wechat.Item;
import cn.btimes.model.wechat.MaterialResult;
import cn.btimes.model.wechat.News;
import cn.btimes.service.ApiRequest;
import cn.btimes.utils.Common;
import com.alibaba.fastjson.JSON;
import com.amzass.model.common.ActionLog;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.google.inject.Inject;
import com.kber.commons.DBManager;
import com.mailman.model.common.WebApiResult;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/1/8 13:12
 */
public class WeChat extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private WebDriver driver;
    @Inject private DBManager dbManager;
    @Inject private ApiRequest apiRequest;

    @Override
    public void execute(WebDriver driver, Config config) {
        try {
            this.initContext(config);
            this.driver = driver;

            logger.info("Fetching WeChat articles.");
            MaterialResult result = this.fetchArticles();
            logger.info("WeChat articles result: Items={}, Total={}", result.getItemCount(), result.getTotalCount());
            if (result.getTotalCount() == 0 || result.getItemCount() == 0) {
                logger.error("WeChat articles not fetched.");
                return;
            }
            this.saveArticles(result);
        } catch (BusinessException e) {
            logger.error("Wechat crawler is interrupted", e);
        }
    }

    private void saveArticles(MaterialResult result) {
        List<Item> items = result.getItems();
        for (Item item : items) {
            Content content = item.getContent();
            List<News> list = content.getNewsList();
            for (News news : list) {
                String logId = Common.toMD5(news.getUrl());
                ActionLog log = dbManager.readById(logId, ActionLog.class);
                if (log != null) {
                    logger.info("Article saved already: {} -> {}", news.getTitle(), news.getUrl());
                    continue;
                }
                try {
                    this.saveNews(news);
                    dbManager.save(new ActionLog(logId), ActionLog.class);
                } catch (BusinessException e) {
                    logger.error("Unable to save article: {} -> {}", news.getTitle(), news.getUrl(), e);
                }
            }
        }
    }

    private void saveNews(News news) {
        Article article = new Article();
        article.setCategory(Category.General);
        article.setTitle(news.getTitle());
        article.setSummary(news.getDigest());
        article.setUrl(news.getUrl());
        article.setDate(new Date());
        String content = StringUtils.replace(news.getContent(), "data-src", "src");
        article.setContent(content);
        Document doc = Jsoup.parse(content);
        doc.select("img[src*=bdWEcuuSicwNa9ib2ZZBOiaxEtP5wgrKicEqv5U8Ze39MrYsQ4emNd0grU7ibB5f8icqLG8XFfiahYbicgVkVOgYiab3dFA]").remove();

        this.fetchContentImages(article, doc);
        this.saveArticle(article, driver);
    }

    private MaterialResult fetchArticles() {
        WebApiResult result = apiRequest.get("/article/fetchWeChatArticles", config);
        if (result == null || StringUtils.isBlank(result.getData())) {
            throw new BusinessException("Unable to fetch WeChat articles.");
        }

        return JSON.parseObject(result.getData(), MaterialResult.class);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return null;
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
        return null;
    }

    @Override
    protected int getSourceId() {
        return 1;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        return null;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {

    }
}
