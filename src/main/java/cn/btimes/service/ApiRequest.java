package cn.btimes.service;

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
    private static final String WEB_API_ENDPOINT = "https://www.businesstimes.cn/api";

    public WebApiResult get(String path) {
        return this.send(path, Method.GET, "");
    }

    public WebApiResult post(String path, String dataText) {
        return this.send(path, Method.POST, dataText);
    }

    public static String getFullUrl(String path) {
        return WEB_API_ENDPOINT + path;
    }

    private WebApiResult send(String path, Method method, String dataText) {
        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            try {
                String result = Jsoup.connect(getFullUrl(path)).ignoreContentType(true)
                    .validateTLSCertificates(false)
                    .data("data", dataText)
                    .method(method).timeout(WaitTime.SuperLong.valInMS()).maxBodySize(0).execute().body();
                WebApiResult resultObj = JSON.parseObject(result, WebApiResult.class);

                if (ReturnCode.notFail(resultObj.getCode())) {
                    return resultObj;
                }
                LOGGER.error("Upload Record result failed: {} -> {}", path, resultObj.getMessage());
                break;
            } catch (IOException e) {
                LOGGER.error("Upload Record result failed: {}", path, e);
                if (i < Constants.MAX_REPEAT_TIMES - 1) {
                    WaitTime.Short.execute();
                }
            } catch (JSONException e) {
                LOGGER.error("Invalid json response: {}", path);
            } catch (Exception e) {
                LOGGER.error("Unexpected exception occurred while uploading record: {}", path, e);
                return null;
            }
        }
        return null;
    }
}
