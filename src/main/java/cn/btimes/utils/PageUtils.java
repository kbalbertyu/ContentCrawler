package cn.btimes.utils;

import com.amzass.enums.common.Directory;
import com.amzass.utils.PageLoadHelper;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Constants;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.amzass.utils.common.JsoupWrapper.WebRequest;
import com.amzass.utils.common.Tools;
import org.apache.commons.io.FileUtils;
import org.jsoup.nodes.Document;
import org.openqa.selenium.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-11 5:35 AM
 */
public class PageUtils extends com.amzass.utils.common.PageUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(PageUtils.class);

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
        try {
            JavascriptExecutor jse = (JavascriptExecutor) driver;
            jse.executeScript("var elem = document.getElementsByClassName('" + className + "')[0];"
                + "elem.parentNode.removeChild(elem);");
        } catch (WebDriverException e) {
            LOGGER.error("Unable to remove element by class: {}", className);
        }
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

    public static void scrollBy(WebDriver driver, long by) {
        JavascriptExecutor jse = (JavascriptExecutor) driver;
        jse.executeScript("window.scrollBy(0, " + by + ");");
        WaitTime.Shortest.execute();
    }

    public static void scrollToElement(WebDriver driver, By by) {
        WebElement element = PageLoadHelper.findElement(driver, by, WaitTime.Shortest);
        if (element == null) return;
        scrollToElement(driver, element);
    }

    public static void scrollToElement(WebDriver driver, WebElement element) {
        if (element == null) {
            return;
        }
        JavascriptExecutor jse = (JavascriptExecutor) driver;
        jse.executeScript("arguments[0].scrollIntoView(true);", element);
        WaitTime.Shortest.execute();
    }

    public static Document getDocumentByJsoup(String url) {
        BusinessException exception = null;
        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            try {
                return new WebRequest(url).submit().document;
            } catch (Exception e) {
                exception = new BusinessException(e);
                LOGGER.error("Failed to load url of: {} -> {}", i + 1, url, e);
                if (i < Constants.MAX_REPEAT_TIMES - 1) {
                    PageLoadHelper.WaitTime.Shorter.execute();
                }
            }
        }

        throw exception;
    }

    public static void savePage(Document doc, String fileName) {
        File file = FileUtils.getFile(Directory.Tmp.path(), fileName);
        Tools.writeStringToFile(file, doc.outerHtml());
    }

    public static void click(WebDriver driver, By by) {
        PageUtils.scrollToElement(driver, by);
        com.amzass.utils.common.PageUtils.click(driver, by);
    }

    public static void click(WebDriver driver, WebElement element) {
        PageUtils.scrollToElement(driver, element);
        com.amzass.utils.common.PageUtils.click(driver, element);
    }

    public static void openInNewTab(WebDriver driver, String url) {
        JavascriptExecutor jse = (JavascriptExecutor) driver;
        jse.executeScript("window.open('" + url + "')");
        WaitTime.Short.execute();
    }
}
