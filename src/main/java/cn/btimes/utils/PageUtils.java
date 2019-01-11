package cn.btimes.utils;

import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Constants;
import com.amzass.utils.common.Tools;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-11 5:35 AM
 */
public class PageUtils extends com.amzass.utils.common.PageUtils {

    public static void loadLazyContent(WebDriver driver) {
        scrollToBottom(driver);
        scrollToTop(driver);
        long height = getPageHeight(driver);

        long to = 0L;
        long by = 300L;
        while (Long.compare(to, height) == -1) {
            scrollBy(driver, by);
            height = getPageHeight(driver);
            to += by;
        }
    }

    public static void removeElementByClass(WebDriver driver, String className) {
        JavascriptExecutor jse = (JavascriptExecutor) driver;
        jse.executeScript("var elem = document.getElementsByClassName('" + className + "')[0];"
            + "elem.parentNode.removeChild(elem);");
    }

    private static long getPageHeight(WebDriver driver) {
        JavascriptExecutor jse = (JavascriptExecutor) driver;
        return (Long) jse.executeScript("return document.body.scrollHeight;");
    }

    private static void scrollToTop(WebDriver driver) {
        for (int j = 0; j < Constants.MAX_REPEAT_TIMES; j++) {
            JavascriptExecutor jse = (JavascriptExecutor) driver;
            jse.executeScript("window.scrollTo(0, 0);");
            Tools.sleep(com.amzass.utils.common.PageUtils.SIMPLE_WAIT_MS);
        }
    }

    private static void scrollBy(WebDriver driver, long by) {
        JavascriptExecutor jse = (JavascriptExecutor) driver;
        jse.executeScript("window.scrollBy(0, " + by + ");");
        WaitTime.Shortest.execute();
    }
}
