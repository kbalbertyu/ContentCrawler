package cn.btimes.utils;

import cn.btimes.model.common.Application;
import cn.btimes.model.common.Config;
import cn.btimes.model.common.ImageType;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.amzass.enums.common.Directory;
import com.amzass.utils.common.Constants;
import com.amzass.utils.common.Tools;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 9/5/2018 5:48 PM
 */
public class Common {

    private static final CharSequence DOUBLE_DOT = "..";

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
        if (StringUtils.isBlank(pageUrl) ||
            StringUtils.startsWith(StringUtils.lowerCase(url), Constants.HTTP)) {
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

        if (StringUtils.startsWith(url, DOUBLE_DOT)) {
            url = baseUrl + (path.length() == 1 ? "" : path) + Constants.SLASH + url;
        } else if (StringUtils.startsWith(url, Constants.FULL_STOP)) {
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

    public static void convertImageFileTypeFromUrl(String url, String path, ImageType targetType) throws IOException {
        BufferedImage im = ImageIO.read(new URL(url));
        ImageIO.write(im, targetType.toExt(), FileUtils.getFile(path));
    }

    public static void convertImageFileType(String path, ImageType targetType) throws IOException {
        FileUtils.moveFile(FileUtils.getFile(path), FileUtils.getFile(path));
        BufferedImage im = ImageIO.read(FileUtils.getFile(path));
        ImageIO.write(im, targetType.toExt(), FileUtils.getFile(path));
    }

    public static Config loadApplicationConfig(Application application) {
        String configStr = Tools.readFileToString(FileUtils.getFile(Directory.Customize.path(), "application.json"));
        HashMap<Application, Config> configs = JSONObject.parseObject(configStr, new TypeReference<HashMap<Application, Config>>() {
        });
        Config config = configs.get(application);
        config.setApplication(application);
        config.init();
        return config;
    }

    public static Config loadApplicationConfig(String appName) {
        Application application = Application.determineApplication(appName);
        return Common.loadApplicationConfig(application);
    }

    private static double calcSimilarity(String text1, String text2) {
        if (StringUtils.containsIgnoreCase(text1, text2) || StringUtils.containsIgnoreCase(text2, text1)) {
            return 1d;
        }
        char[] texts1 = text1.toCharArray();
        char[] texts2 = text2.toCharArray();
        Arrays.sort(texts1);
        Arrays.sort(texts2);
        int count = sameChars(texts1, texts2);
        int total = (texts1.length + texts2.length)/2;

        return (double) Math.round(count/total * 100) / 100;
    }

    private static int sameChars(char[] arr1, char[] arr2) {
        LinkedList<Character> list = new LinkedList<>();
        for (char str : arr1) {
            if(!list.contains(str)) {
                list.add(str);
            }
        }

        for (char str : arr2) {
            if (list.contains(str)) {
                list.remove(str);
            }
        }
        return list.size();
    }

    public static String getFileExtension(String fileName) {
        fileName = StringUtils.substringBefore(fileName, "?");
        String[] parts = StringUtils.split(fileName, ".");
        return parts[parts.length - 1].toLowerCase();
    }

    public static String trim(String text) {
        return StringUtils.trim(StringUtils.removePattern(text, "^[　*| *| *|//s*]*"));
    }

    public static void main(String[] args) {
        String text1 = "纽约时装周";
        String text2 = "伦敦时装周活动";
        System.out.println(calcSimilarity(text1, text2));
    }
}
