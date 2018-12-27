package cn.btimes.utils;

import org.apache.commons.codec.digest.DigestUtils;

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
}
