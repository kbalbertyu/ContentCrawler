package cn.btimes.service.nlp;

import cn.btimes.model.nlp.Tag;
import cn.btimes.utils.Tools;
import com.baidu.aip.nlp.AipNlp;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-05-23 9:28 AM
 */
public class BaiduNLP extends AbstractNLP {
    private final Logger logger = LoggerFactory.getLogger(BaiduNLP.class);
    private static final String APP_ID = "11252541";
    private static final String API_KEY = "rog1wt9MYToYk3NNyTNhuDuA";
    private static final String SECRET_KEY = "WUNPS3wPKjhIkMhHw9pRxfOOuEQ2Rf4A";
    private static final AipNlp client = initClient();

    private static AipNlp initClient() {
        // 初始化一个AipNlp
        AipNlp client = new AipNlp(APP_ID, API_KEY, SECRET_KEY);

        // 可选：设置网络连接参数
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);
        return client;
    }

    @Override
    public List<Tag> generateTags(String title, String content) {
        List<Tag> tags = new ArrayList<>();

        // 传入可选参数调用接口
        HashMap<String, Object> options = new HashMap<>();
        String[] rows = StringUtils.split(content);

        for (String row : rows) {
            row = StringUtils.trim(row);
            if (StringUtils.length(row) <= 5) {
                continue;
            }
            Tools.baiduServiceWait();
            JSONObject res = client.keyword(title, row, options);
            try {
                String items = res.getString("items");
                List<Tag> keywordList = com.alibaba.fastjson.JSONObject.parseArray(items, Tag.class);
                if (keywordList.size() == 0) {
                    continue;
                }
                logger.info("Tags parsed in row: {}", keywordList);
                tags.addAll(keywordList);
            } catch (JSONException e) {
                logger.error("Unable to parse keywords from result: {}", res, e);
            }
        }

        return tags;
    }

    /**
     * 短文本相似度
     */
    public Double similarLiteral(String text1, String text2) {
        Tools.baiduServiceWait();
        HashMap<String, Object> options = new HashMap<>();
        options.put("model", "CNN");

        // 短文本相似度
        JSONObject res = client.simnet(text1, text2, options);
        try {
            return res.getDouble("score");
        } catch (JSONException e) {
            logger.error("Unable to parse score from result: {}", res, e);
        }
        return 0d;
    }

    /**
     * 词义相似度
     */
    public Double similar(String text1, String text2) {
        Tools.baiduServiceWait();
        HashMap<String, Object> options = new HashMap<>();
        options.put("mode", 0);

        // 词义相似度
        JSONObject res = client.wordSimEmbedding(text1, text2, options);
        try {
            return res.getDouble("score");
        } catch (JSONException e) {
            logger.error("Unable to parse score from result: {}", res, e);
        }
        return 0d;
    }
}
