package cn.btimes.service;

import cn.btimes.model.common.Config;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.google.inject.Inject;
import com.mailman.model.common.WebApiResult;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/8/4 22:26
 */
public class BTContentFixHandler implements ServiceExecutorInterface {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Inject private ApiRequest apiRequest;

    @Override
    public void execute(Config config) {
        List<Integer> ids = this.fetchAids(config);
        if (CollectionUtils.isEmpty(ids)) {
            logger.error("No articles found.");
            return;
        }

        int size = ids.size();
        logger.info("Found {} articles", size);
        int length = 100;

        Map<Integer, String> contents = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            int from = i * length;
            if (from >= size) {
                break;
            }
            int to = (i + 1) * length;
            to = Integer.min(size - 1, to);
            List<Integer> subIds = ids.subList(from, to);

            Map<Integer, String> subContents = this.fetchContentsInGroup(subIds, config);
            if (subContents == null || subContents.size() == 0) {
                logger.error("No article contents found");
                continue;
            }
            logger.info("Found {} article contents on item: {} of {}", subContents.size(), to, size);
            contents.putAll(subContents);
        }

        for (int id : contents.keySet()) {
            String content = contents.get(id);
            this.uploadContent(id, content, config);
        }
    }

    private void uploadContent(int id, String content, Config config) {
        WebApiResult result = apiRequest.post("/article/updateArticleContent?id=" + id, content, config);
        if (result == null) {
            logger.error("Unable to upload the content: {}", id);
            return;
        }
        logger.info("Content uploaded: {}", id);
    }

    private Map<Integer, String> fetchContentsInGroup(List<Integer> ids, Config config) {
        WebApiResult result = apiRequest.post("/article/fetchContents", JSON.toJSONString(ids), config);
        if (result == null || StringUtils.isBlank(result.getData())) {
            return null;
        }
        return JSON.parseObject(result.getData(), new TypeReference<Map<Integer, String>>() {
        });
    }

    /**
     * @deprecated
     */
    private Map<Integer, String> fetchContents(List<Integer> ids, Config config) {
        Map<Integer, String> contents = new HashMap<>();
        for (int id : ids) {
            WebApiResult result = apiRequest.get("/article/fetchContents?id=" + id, config);
            if (result == null || StringUtils.isBlank(result.getData())) {
                logger.warn("Article content not found: {}", id);
                return null;
            }
            logger.info("Article content found: {}", id);
            contents.put(id, result.getData());
        }
        return contents;
    }

    private List<Integer> fetchAids(Config config) {
        WebApiResult result = apiRequest.get("/article/fetchArticlesWithContentError", config);
        if (result == null || StringUtils.isBlank(result.getData())) {
            return null;
        }

        return JSON.parseArray(result.getData(), Integer.class);
    }
}
