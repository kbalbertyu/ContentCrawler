package cn.btimes.service;

import cn.btimes.model.common.Config;
import cn.btimes.model.common.TagIndex;
import cn.btimes.utils.Tools;
import com.alibaba.fastjson.JSONObject;
import com.amzass.model.common.ActionLog;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.google.inject.Inject;
import com.kber.commons.DBManager;
import com.mailman.model.common.WebApiResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-07-12 2:54 PM
 */
public class RelatedArticleHandler implements ServiceExecutorInterface {
    private final Logger logger = LoggerFactory.getLogger(TagGenerator.class);
    @Inject private ApiRequest apiRequest;
    @Inject private DBManager dbManager;
    private static final String DAYS_BEFORE = Tools.getCustomizingValue("RELATED_DAYS_BEFORE");

    @Override
    public void execute(Config config) {
        WebApiResult result = apiRequest.get("/article/fetchRecentTagIndex?days=" + DAYS_BEFORE, config);
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
            if (counts.size() == 0) {
                continue;
            }
            if (this.saveRelatedArticles(aid, counts, config)) {
                logger.info("Related articles imported: {}, {} of {}", aid, i++, total);
            } else {
                logger.error("No related articles imported: {}, {} of {}", aid, i++, total);
            }
        }
    }

    private boolean saveRelatedArticles(int aid, Map<Integer, Integer> counts, Config config) {
        if (counts.size() == 0) {
            return false;
        }
        List<String> data = new ArrayList<>();
        Set<String> logIds = new HashSet<>();
        for (Integer articleId : counts.keySet()) {
            String logId = "related-" + aid + ":" + articleId;
            ActionLog log = dbManager.readById(logId, ActionLog.class);
            if (log != null) {
                continue;
            }
            logIds.add(logId);
            data.add(articleId + ":" + counts.get(articleId));
        }
        if (data.size() == 0) {
            return false;
        }

        WaitTime.Normal.execute();
        String dataText = StringUtils.join(data, "|");
        WebApiResult result = apiRequest.post("/article/saveRelatedArticles?aid=" + aid, dataText, config);
        if (result == null) {
            String message = String.format("Unable to save related articles: %d -> %s", aid, dataText);
            logger.error(message);
            return false;
        } else {
            for (String logId : logIds) {
                dbManager.save(new ActionLog(logId), ActionLog.class);
            }
        }
        return true;
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
