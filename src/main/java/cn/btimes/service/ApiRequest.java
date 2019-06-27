package cn.btimes.service;

import cn.btimes.model.common.Config;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.amzass.model.submit.OrderEnums.ReturnCode;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Constants;
import com.mailman.model.common.WebApiResult;
import com.mailman.service.common.WebApiRequest;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-02-01 1:38 AM
 */
public class ApiRequest extends WebApiRequest {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebApiRequest.class);
    private static final String WEB_API_ENDPOINT = "/api";

    public WebApiResult get(String path, Config config) {
        return this.send(config, path, Method.GET, "");
    }

    public WebApiResult get(String url) {
        return this.send(url, Method.GET, "");
    }

    public WebApiResult post(String path, String dataText, Config config) {
        return this.send(config, path, Method.POST, dataText);
    }

    private static String getFullUrl(String path, Config config) {
        return config.getFrontUrl() + WEB_API_ENDPOINT + path;
    }

    private WebApiResult send(Config config, String path, Method method, String dataText) {
        return this.send(getFullUrl(path, config), method, dataText);
    }

    private WebApiResult send(String url, Method method, String dataText) {
        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            try {
                String result = Jsoup.connect(url).ignoreContentType(true)
                    .validateTLSCertificates(false)
                    .data("data", dataText)
                    .method(method).timeout(WaitTime.SuperLong.valInMS()).maxBodySize(0).execute().body();
                WebApiResult resultObj = JSON.parseObject(result, WebApiResult.class);

                if (ReturnCode.notFail(resultObj.getCode())) {
                    return resultObj;
                }
                LOGGER.error("Upload Record result failed: {} -> {}", url, resultObj.getMessage());
                break;
            } catch (IOException e) {
                LOGGER.error("Upload Record result failed: {}", url, e);
                if (i < Constants.MAX_REPEAT_TIMES - 1) {
                    WaitTime.Short.execute();
                }
            } catch (JSONException e) {
                LOGGER.error("Invalid json response: {}", url);
            } catch (Exception e) {
                LOGGER.error("Unexpected exception occurred while uploading record: {}", url, e);
                return null;
            }
        }
        return null;
    }
}
