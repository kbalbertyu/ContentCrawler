package cn.btimes.service;

import cn.btimes.model.common.Config;
import cn.btimes.model.nlp.Tag;
import cn.btimes.model.nlp.TagSimilarity;
import cn.btimes.service.ai.BaiduNLP;
import cn.btimes.utils.Common;
import com.alibaba.fastjson.JSON;
import com.amzass.service.common.ApplicationContext;
import com.google.inject.Inject;
import com.mailman.model.common.WebApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-05-24 2:48 PM
 */
public class TagSimilar extends TagHandler {
    private final Logger logger = LoggerFactory.getLogger(TagGenerator.class);
    @Inject private ApiRequest apiRequest;
    @Inject private BaiduNLP baiduNLP;

    public void execute(Config config) {
        List<Tag> tags = this.readTags("/article/fetchArticlesTags?banned=1", config);

        List<TagSimilarity> tagSimilarities = new ArrayList<>();
        for (int i = 0; i < tags.size(); i++) {
            Tag tag1 = tags.get(i);
            for (int j = 0; j < tags.size(); j++) {
                Tag tag2 = tags.get(j);
                if (i == j) {
                    continue;
                }
                double scoreDistance = baiduNLP.similar(tag1.getTag(), tag2.getTag());
                double scoreLiteral = baiduNLP.similarLiteral(tag1.getTag(), tag2.getTag());

                TagSimilarity tagSimilarity = new TagSimilarity(tag1.getId(), tag2.getId(), scoreDistance, scoreLiteral);
                tagSimilarities.add(tagSimilarity);
            }
        }
        WebApiResult importResult = apiRequest.post("/article/import-tag-similarity", JSON.toJSONString(tagSimilarities));
        if (importResult == null) {
            logger.error("Unable to import tag similarity records.");
        } else {
            logger.info("Tag similarity records import successfully.");
        }
    }

    public static void main(String[] args) {
        String appName = args.length > 0 ? args[0] : null;
        Config config = Common.loadApplicationConfig(appName);
        ApplicationContext.getBean(TagSimilar.class).execute(config);
        System.exit(0);
    }
}
