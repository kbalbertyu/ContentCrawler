package cn.btimes.service;

import cn.btimes.model.common.Config;
import com.alibaba.fastjson.JSONObject;
import com.amzass.model.common.ActionLog;
import com.google.inject.Inject;
import com.kber.commons.DBManager;
import com.mailman.model.common.WebApiResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/8/2 8:37
 */
abstract class AbstractLinksUploader implements ServiceExecutorInterface {
    private final Logger logger = LoggerFactory.getLogger(AbstractLinksUploader.class);
    @Inject private ApiRequest apiRequest;
    @Inject private DBManager dbManager;

    /**
     * Fetch links for submission
     */
    List<String> fetchLinks(String fetchUrl, Config config, String urlIdPrefix) {
        WebApiResult result = apiRequest.get(fetchUrl, config);
        if (result == null || StringUtils.isBlank(result.getData())) {
            return null;
        }
        List<String> urls = JSONObject.parseArray(result.getData(), String.class);
        if (urls.size() == 0) {
            return null;
        }

        urls.removeIf(url -> dbManager.readById(urlId(url, urlIdPrefix), ActionLog.class) != null);
        if (urls.size() == 0) {
            return null;
        }
        return urls;
    }

    /**
     * <a href="https://liqita.iteye.com/blog/2221082">Reference</a>
     */
    String postLinks(String postUrl, String data, String apiHost) {
        StringBuilder result = new StringBuilder();
        PrintWriter out = null;
        BufferedReader in = null;
        try {
            //建立URL之间的连接
            URLConnection conn = new URL(postUrl).openConnection();
            //设置通用的请求属性
            conn.setRequestProperty("Host", apiHost);
            conn.setRequestProperty("User-Agent", "curl/7.12.1");
            conn.setRequestProperty("Content-Length", String.valueOf(data.length()));
            conn.setRequestProperty("Content-Type", "text/plain");

            //发送POST请求必须设置如下两行
            conn.setDoInput(true);
            conn.setDoOutput(true);

            //获取conn对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            //发送请求参数
            out.print(data);
            //进行输出流的缓冲
            out.flush();
            //通过BufferedReader输入流来读取Url的响应
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (Exception e) {
            logger.error("Unable to send out the SiteMap urls.", e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                logger.error("Unable to close stream.");
            }
        }
        return result.toString();
    }

    List<String> urlsSegment(List<String> urls, int size, int i, int itemsPerUpload) {
        int start = i * itemsPerUpload;
        if (start >= size) {
            return null;
        }
        int end = (i + 1) * itemsPerUpload;
        if (end > size) {
            end = size;
        }
        return urls.subList(start, end);
    }

    static String urlId(String url, String prefix) {
        return (StringUtils.isBlank(prefix) ? "" : prefix + "-") + url;
    }
}
