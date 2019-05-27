package cn.btimes.service;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.Config;
import cn.btimes.model.nlp.Tag;
import cn.btimes.service.nlp.AbstractNLP;
import cn.btimes.service.nlp.BaiduNLP;
import cn.btimes.service.nlp.IFlyNLP;
import cn.btimes.utils.Common;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.amzass.service.common.ApplicationContext;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.google.inject.Inject;
import com.mailman.model.common.WebApiResult;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-05-23 9:07 AM
 */
public class TagGenerator {
    private final Logger logger = LoggerFactory.getLogger(TagGenerator.class);
    private final List<AbstractNLP> nlpList = this.initNLPList();
    @Inject private ApiRequest apiRequest;

    private List<AbstractNLP> initNLPList() {
        List<AbstractNLP> nlpList = new ArrayList<>();
        nlpList.add(ApplicationContext.getBean(BaiduNLP.class));
        nlpList.add(ApplicationContext.getBean(IFlyNLP.class));
        return nlpList;
    }

    private List<Tag> execute(String title, String content) {
        for (AbstractNLP nlp : nlpList) {
            try {
                return nlp.generateTags(title, content);
            } catch (BusinessException ignored) {
            }
        }
        return null;
    }

    private void execute(Config config) {
        int page = 10;
        while (true) {
            page++;
            logger.info("Executing page: {}", page);
            WebApiResult result = apiRequest.get("/article/fetchArticlesForTags?page=" + page, config);
            if (result == null) {
                logger.error("Article result not found on page: {}", page);
                return;
            }
            List<Article> articles = JSONObject.parseArray(result.getData(), Article.class);
            if (articles.size() == 0) {
                logger.error("No article found on page: {}", page);
                return;
            }
            for (Article article : articles) {
                logger.info("Parsing tags for article: {} -> {}", article.getId(), article.getTitle());
                List<Tag> tags = this.execute(article.getTitle(), article.getContent());
                if (CollectionUtils.isEmpty(tags)) {
                    logger.error("No tags parsed for article: {} -> {}", article.getId(), article.getTitle());
                    continue;
                }
                logger.info("Importing tags for article: {} -> {}", article.getId(), tags);
                this.filterTags(tags);
                this.importTags(article.getId(), tags, config);
            }
        }
    }

    private void filterTags(List<Tag> tags) {
        List<String> existing = new ArrayList<>();
        Iterator<Tag> iterator = tags.iterator();
        while (iterator.hasNext()) {
            Tag tag = iterator.next();
            if (StringUtils.isBlank(tag.getTag())) {
                iterator.remove();
                continue;
            }
            if (!existing.contains(tag.getTag())) {
                existing.add(tag.getTag());
                continue;
            }
            iterator.remove();
        }
    }

    private void importTags(int id, List<Tag> tags, Config config) {
        Map<String, Double> map = new HashMap<>();
        for (Tag tag : tags) {
            map.put(tag.getTag(), tag.getScore());
        }
        WebApiResult result = apiRequest.post("/article/importTags?ar_id=" + id, JSON.toJSONString(map), config);
        if (result == null) {
            logger.error("Unable to save tags of article: {}", id);
        } else {
            logger.info("Article tags saved: {} -> {}", id, result.getData());
        }
    }

    public static void main(String[] args) {
        String appName = args.length > 0 ? args[0] : null;
        Config config = Common.loadApplicationConfig(appName);
        ApplicationContext.getBean(TagGenerator.class).execute(config);
        System.exit(0);
    }
}
