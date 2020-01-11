package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import cn.btimes.model.common.Config;
import cn.btimes.model.wechat.*;
import cn.btimes.service.EmailSenderHelper;
import cn.btimes.utils.Common;
import cn.btimes.utils.Tools;
import com.alibaba.fastjson.JSON;
import com.amzass.enums.common.Directory;
import com.amzass.model.common.ActionLog;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.google.inject.Inject;
import com.kber.commons.DBManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/1/8 13:12
 */
public class WeChat extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String WECHAT_CONFIG_FILE = "wechat.json";
    private static final String WECHAT_API_ENDPOINT = "https://api.weixin.qq.com/cgi-bin";
    private static final String WECHAT_TOKEN_PATH = "%s/token?grant_type=client_credential&appid=%s&secret=%s";
    private static final String WECHAT_ARTICLE_LIST_PATH = "%s/material/batchget_material?access_token=%s";
    private AppConfig wechat;
    private WebDriver driver;
    @Inject private DBManager dbManager;
    @Inject private EmailSenderHelper emailSenderHelper;

    @Override
    public void execute(WebDriver driver, Config config) {
        try {
            this.initContext(config);
            this.driver = driver;
            this.loadConfig();
            if (wechat.tokenExpired()) {
                logger.info("WeChat token expired, request the token.");
                this.requestToken();
            }
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
                    this.saveNews(news, item.getUpdateTime());
                    dbManager.save(new ActionLog(logId), ActionLog.class);
                } catch (BusinessException e) {
                    logger.error("Unable to save article: {} -> {}", news.getTitle(), news.getUrl(), e);
                }
            }
        }
    }

    private void saveNews(News news, long updateTime) {
        Article article = new Article();
        article.setCategory(Category.General);
        article.setTitle(news.getTitle());
        article.setSummary(news.getDigest());
        article.setUrl(news.getUrl());
        Date date = new Date();
        date.setTime(updateTime * 1000);
        article.setDate(date);
        String content = StringUtils.replace(news.getContent(), "data-src", "src");
        article.setContent(content);
        Document doc = Jsoup.parse(content);
        this.fetchContentImages(article, doc);
        this.saveArticle(article, driver);
    }

    private MaterialResult fetchArticles() {
        String url = String.format(WECHAT_ARTICLE_LIST_PATH, WECHAT_API_ENDPOINT, wechat.getToken());
        String data = "{\"type\":\"news\", \"offset\":0, \"count\":20}";
        String body = sendHttpPost(url, data);
        return JSON.parseObject(body, MaterialResult.class);
    }

    private static String sendHttpPost(String url, String body) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("Content-Type", "application/json");
        try {
            httpPost.setEntity(new StringEntity(body));
        } catch (UnsupportedEncodingException e) {
            throw new BusinessException(e);
        }
        try {
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            String responseContent = EntityUtils.toString(entity, "UTF-8");
            response.close();
            httpClient.close();
            return responseContent;
        } catch (IOException e) {
            throw new BusinessException(e);
        }
    }

    private void requestToken() {
        String url = String.format(WECHAT_TOKEN_PATH, WECHAT_API_ENDPOINT, wechat.getAppId(), wechat.getAppSecret());
        try {
            String body = Jsoup.connect(url).method(Method.POST)
                .ignoreContentType(true)
                .execute().body();

            logger.info("Token result: {}", body);
            Token token = JSON.parseObject(body, Token.class);
            if (token.invalid()) {
                String message = String.format("Unable to request the token: %s", token.getErrmsg());
                this.emailSenderHelper.send("[%s]WeChat token request failed", config.getApplication().name(),
                    message, config.getDeveloperEmail(), config.getRecipient());
                throw new BusinessException(message);
            }
            wechat.updateToken(token);
            File file = FileUtils.getFile(Directory.Customize.path(), WECHAT_CONFIG_FILE);
            Tools.writeStringToFile(file, JSON.toJSONString(wechat));
        } catch (IOException e) {
            logger.error("Unable to request to the token:", e);
            throw new BusinessException(e);
        }
    }

    private void loadConfig() {
        String content = Tools.readFileToString(FileUtils.getFile(Directory.Customize.path(), WECHAT_CONFIG_FILE));
        this.wechat = JSON.parseObject(content, AppConfig.class);
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
