package cn.btimes.utils;

import cn.btimes.model.common.ImageType;
import com.amzass.utils.common.Constants;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

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

    public static String percentage(float val) {
        return Constants.DOUBLE_FORMAT.format(val) + Constants.PERCENTAGE;
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
            url = baseUrl + path + Constants.SLASH + url;
        }
        return url;
    }

    public static String extractFileNameFromUrl(String url) {
        url = StringUtils.substringBefore(url, "?");
        String[] pathParts = StringUtils.split(url, "/");
        return pathParts[pathParts.length - 1];
    }

    public static ImageType determineImageFileType(String file) throws IOException {
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            byte[] b = new byte[3];
            is.read(b, 0, b.length);
            String hex = bytesToHexString(b).toUpperCase();
            return ImageType.getTypeByHex(hex);
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        for (byte aSrc : src) {
            int v = aSrc & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    public static String getDomain(String url) {
        String[] parts = StringUtils.split(url, "/");
        return parts[1];
    }

    public static void convertImageFileType(String originalUrl, String path, ImageType targetType) throws IOException {
        FileUtils.deleteQuietly(FileUtils.getFile(path));
        BufferedImage im = ImageIO.read(new URL(originalUrl));
        ImageIO.write(im, targetType.toExt(), FileUtils.getFile(path));
    }
}
