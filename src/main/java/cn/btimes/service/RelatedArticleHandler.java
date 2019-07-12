package cn.btimes.service;

import cn.btimes.model.common.Config;
import cn.btimes.model.common.TagIndex;
import com.alibaba.fastjson.JSONObject;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.google.inject.Inject;
import com.mailman.model.common.WebApiResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-07-12 2:54 PM
 */
public class RelatedArticleHandler implements ServiceExecutorInterface {
    private final Logger logger = LoggerFactory.getLogger(TagGenerator.class);
    @Inject private ApiRequest apiRequest;

    @Override
    public void execute(Config config) {
        WebApiResult result = apiRequest.get("/article/fetchAllTagIndex", config);
        String data = result.getData();
        List<TagIndex> list = JSONObject.parseArray(data, TagIndex.class);

        Map<Integer, List<Integer>> aidIndexMap = this.toArticleTagIndex(list);
        Map<Integer, List<Integer>> tagIndexMap = this.toTagArticleIndex(list);

        int total = aidIndexMap.size();
        int i = 1;
        for (int aid : aidIndexMap.keySet()) {
            List<Integer> tags = aidIndexMap.get(aid);
            Map<Integer, Integer> counts = new HashMap<>();
            for (int tag : tags) {
                List<Integer> aids = tagIndexMap.get(tag);
                for (int aidOther : aids) {
                    if (aid == aidOther) {
                        continue;
                    }
                    int count = counts.getOrDefault(aidOther, 0);
                    counts.put(aidOther, count + 1);
                }
            }
            this.saveRelatedArticles(aid, counts, config);
            logger.info("Related articles imported: {}, {} of {}", aid, i++, total);
        }
    }

    private void saveRelatedArticles(int aid, Map<Integer, Integer> counts, Config config) {
        String[] data = new String[counts.size()];
        int i = 0;
        for (Integer articleId : counts.keySet()) {
            data[i] = articleId + ":" + counts.get(articleId);
            i++;
        }
        WebApiResult result = apiRequest.post("/article/saveRelatedArticles?aid=" + aid, StringUtils.join(data, "|"), config);
        if (result == null) {
            String message = String.format("Unable to save related articles: %d", aid);
            logger.error(message);
            throw new BusinessException(message);
        }
    }

    private Map<Integer, List<Integer>> toArticleTagIndex(List<TagIndex> list) {
        Map<Integer, List<Integer>> aidIndexMap = new HashMap<>();
        for (TagIndex tagIndex : list) {
            int aid = tagIndex.getArticleId();
            List<Integer> index = aidIndexMap.getOrDefault(aid, new ArrayList<>());
            index.add(tagIndex.getTagId());
            aidIndexMap.put(aid, index);
        }
        return aidIndexMap;
    }

    private Map<Integer, List<Integer>> toTagArticleIndex(List<TagIndex> list) {
        Map<Integer, List<Integer>> tagIndexMap = new HashMap<>();
        for (TagIndex tagIndex : list) {
            int tagId = tagIndex.getTagId();
            List<Integer> index = tagIndexMap.getOrDefault(tagId, new ArrayList<>());
            index.add(tagIndex.getArticleId());
            tagIndexMap.put(tagId, index);
        }
        return tagIndexMap;
    }
}
