package cn.btimes.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
}
