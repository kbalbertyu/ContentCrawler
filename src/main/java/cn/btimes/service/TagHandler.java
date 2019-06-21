package cn.btimes.service;

import cn.btimes.model.common.Config;
import cn.btimes.model.nlp.Tag;
import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.mailman.model.common.WebApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/6/14 16:34
 */
abstract class TagHandler implements ServiceExecutorInterface {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Inject private ApiRequest apiRequest;

    List<Tag> readTags(String apiPath, Config config) {
        WebApiResult result = apiRequest.get(apiPath, config);
        if (result == null) {
            logger.error("Tas result not found");
            return null;
        }
        List<Tag> tags = JSONObject.parseArray(result.getData(), Tag.class);
        if (tags.size() == 0) {
            logger.error("No tag parsed in result: {}", result.getData());
            return null;
        }
        return tags;
    }
}
