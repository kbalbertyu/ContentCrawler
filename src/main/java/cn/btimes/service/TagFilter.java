package cn.btimes.service;

import cn.btimes.model.common.Config;
import cn.btimes.model.nlp.Tag;
import cn.btimes.service.filter.BaiduFilter;
import cn.btimes.utils.Common;
import com.alibaba.fastjson.JSONObject;
import com.amzass.service.common.ApplicationContext;
import com.google.inject.Inject;
import com.mailman.model.common.WebApiResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-05-24 11:32 AM
 */
public class TagFilter {
    private final Logger logger = LoggerFactory.getLogger(TagFilter.class);
    @Inject private ApiRequest apiRequest;
    @Inject private BaiduFilter baiduFilter;

    private void execute(Config config) {
        WebApiResult result = apiRequest.get("/article/fetchArticlesTags", config);
        if (result == null) {
            logger.error("Tas result not found");
            return;
        }
        List<Tag> tags = JSONObject.parseArray(result.getData(), Tag.class);
        if (tags.size() == 0) {
            logger.error("No tag parsed in result: {}", result.getData());
            return;
        }
        List<Integer> bannedTagIds = new ArrayList<>();
        List<Integer> approvedTagIds = new ArrayList<>();
        for (Tag tag : tags) {
            if (baiduFilter.validate(tag.getTag())) {
                approvedTagIds.add(tag.getId());
                continue;
            }
            bannedTagIds.add(tag.getId());
        }
        if (bannedTagIds.size() != 0) {
            String tagIdsText = StringUtils.join(bannedTagIds, ",");
            result = apiRequest.post("/article/banTags", tagIdsText, config);
            if (result == null) {
                logger.error("Unable to ban the tags: " + tagIdsText);
                return;
            }
            logger.info("Tags are banned successfully");
        }
        if (approvedTagIds.size() != 0) {
            String tagIdsText = StringUtils.join(approvedTagIds, ",");
            result = apiRequest.post("/article/approveTags", tagIdsText, config);
            if (result == null) {
                logger.error("Unable to approve the tags: " + tagIdsText);
                return;
            }
            logger.info("Tags are approved successfully");
        }
    }

    public static void main(String[] args) {
        String appName = args.length > 0 ? args[0] : null;
        Config config = Common.loadApplicationConfig(appName);
        ApplicationContext.getBean(TagFilter.class).execute(config);
        System.exit(0);
    }
}
