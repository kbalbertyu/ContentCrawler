package cn.btimes.service.filter;

import cn.btimes.utils.Tools;
import com.baidu.aip.contentcensor.AipContentCensor;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-05-24 11:34 AM
 */
public class BaiduFilter {
    private final Logger logger = LoggerFactory.getLogger(BaiduFilter.class);
    private static final String APP_ID = "16332058";
    private static final String API_KEY = "f7et49573lGtrB1PeXoyHAAz";
    private static final String SECRET_KEY = "6z3y7uRE5GIKjDvzH0gx28FYnUQb4BYK";
    private static final AipContentCensor client = initClient();

    private static AipContentCensor initClient() {
        // 初始化一个AipImageCensor
        AipContentCensor client = new AipContentCensor(APP_ID, API_KEY, SECRET_KEY);

        // 可选：设置网络连接参数
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);
        return client;
    }

    public boolean validate(String content) {
        Tools.baiduServiceWait();
        // 调用接口
        JSONObject response = client.antiSpam(content, null);
        logger.info("Baidu filter result: {}", response);
        try {
            JSONObject result = response.getJSONObject("result");
            return result.getInt("spam") == 0;
        } catch (JSONException e) {
            logger.error("Unable to parse validation result: ", response);
        }
        return true;
    }
}
