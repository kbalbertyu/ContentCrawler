package cn.btimes.source;

import cn.btimes.service.WebDriverLauncher;
import com.amzass.utils.common.ProcessCleaner;
import com.google.inject.Inject;
import com.kber.test.BaseTest;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import java.io.IOException;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-24 7:12 PM
 */
public class BasePageTest extends BaseTest {
    static WebDriver driver;
    @Inject private WebDriverLauncher webDriverLauncher;

    @BeforeSuite public void prepare() throws IOException {
        driver = webDriverLauncher.startWithoutLogin();
    }

    @AfterSuite
    public void clean() {
        ProcessCleaner.cleanWebDriver();
    }
}
