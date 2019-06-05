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
        WebApiResult result = apiRequest.get("/article/fetchArticlesTags?status=0", config);
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

    private void test(Config config) {
        String tagIdsText = "166520,166521,166567,166687,166810,166934,167023,167028,167268,167283,167368,167571,167771,168232,168366,168367,168557,168666,169395,169495,169818,170003,170534,170555,170620,170636,170723,171107,171120,171210,171425,172405,172547,172568,172597,172610,172700,172903,173334,174120,174123,174332,174507,174570,174792,175293,175377,175572,175774,175852,176247,176279,176389,176462,176472,176619,176645,177084,177247,177500";
        WebApiResult result = apiRequest.post("/article/banTags", tagIdsText, config);
        System.out.println(result);
    }

    public static void main(String[] args) {
        String appName = args.length > 0 ? args[0] : null;
        Config config = Common.loadApplicationConfig(appName);
        ApplicationContext.getBean(TagFilter.class).test(config);
        System.exit(0);
    }
}
