package cn.btimes.utils;

import com.amzass.utils.common.Constants;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 9/5/2018 5:48 PM
 */
public class Common {

    public static String toMD5(String text) {
        return DigestUtils.md5Hex(text).toUpperCase();
    }

    public static boolean numericEquals(float num1, float num2) {
        return Math.abs(num1 - num2) < 0.001;
    }

    private static final String DOUBLE_DASH = "//";

    public static String getAbsoluteUrl(String url, String pageUrl) {
        if (StringUtils.startsWith(StringUtils.lowerCase(url), Constants.HTTP)) {
            return url;
        }

        String[] parts = StringUtils.split(pageUrl, Constants.SLASH);
        if (StringUtils.startsWith(url, DOUBLE_DASH)) {
            return parts[0] + url;
        }
        String baseUrl = parts[0] + DOUBLE_DASH + parts[1];
        String path;
        if (parts.length == 2) {
            path = "";
        } else {
            boolean endWithDir = StringUtils.endsWith(pageUrl, Constants.SLASH);
            int length = parts.length - (endWithDir ? 0 : 1);

            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < length; i++) {
                sb.append(Constants.SLASH);
                sb.append(parts[i]);
            }
            path = sb.toString();
        }

        if (StringUtils.startsWith(url, Constants.FULL_STOP)) {
            url = baseUrl + (path.length() == 1 ? "" : path) + StringUtils.substring(url, 1);
        } else if (StringUtils.startsWith(url, Constants.SLASH)) {
            url = baseUrl + url;
        } else {
            url = baseUrl + path + url;
        }
        return url;
    }

    public static String extractFileNameFromUrl(String url) {
        String[] pathParts = StringUtils.split(url, "\\/");
        String[] nameParts = StringUtils.split(pathParts[pathParts.length - 1], "?");
        return nameParts[0];
    }
}
