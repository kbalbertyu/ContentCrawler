package cn.btimes.utils;

import com.amzass.utils.common.Constants;
import com.amzass.utils.common.Exceptions.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-05-24 1:52 PM
 */
public class Tools extends com.amzass.utils.common.Tools {
    private static final Logger LOGGER = LoggerFactory.getLogger(Tools.class);

    public static void baiduServiceWait() {
        sleep(200L);
    }

    public static String encodeUrl(String url) {
        String base = StringUtils.substringBefore(url, "?");
        String query = StringUtils.substringAfter(url, "?");
        try {
            return base + "?" + URLEncoder.encode(query, "utf-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Illegal character in url: {}", url, e);
            return url;
        }
    }

    public static File downloadFile(String src, String filePath) {
        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            Response resultImageResponse;
            try {
                resultImageResponse = Jsoup.connect(src)
                    .validateTLSCertificates(false)
                    .ignoreContentType(true).execute();
            } catch (IOException e) {
                LOGGER.error("Unable to build download connection at {} attempt: {} -> ", i + 1, src, e);
                continue;
            }
            try {
                File file = new File(filePath);
                FileOutputStream out = new FileOutputStream(file);
                out.write(resultImageResponse.bodyAsBytes());
                out.close();
                return file;
            } catch (IOException e) {
                LOGGER.error("Unable to download file {} attempt: {} -> ", i + 1, src, e);
            }
        }
        throw new BusinessException(String.format("Failed to download file: %s.", src));
    }
}
