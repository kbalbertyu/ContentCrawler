package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import cn.btimes.service.ApiRequest;
import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.mailman.model.common.WebApiResult;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/8/3 10:38
 */
public class BTimes extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final Map<String, Category> URLS = new HashMap<>();
    @Inject private ApiRequest apiRequest;

    static {
        URLS.put("", Category.ECONOMY);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
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
        return 0;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        WebApiResult result = apiRequest.get("/article/fetchArticlesWithForeignImages", config);
        if (result == null || StringUtils.isBlank(result.getData())) {
            logger.error("Article not found");
            return null;
        }
        return JSONObject.parseArray(result.getData(), Article.class);
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.fetchContentImages(article);
    }

    @Override
    boolean allowArticleWithoutImage() {
        return false;
    }
}
