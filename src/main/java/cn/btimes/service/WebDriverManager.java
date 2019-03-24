package cn.btimes.service;

import com.amzass.enums.common.ConfigEnums.ChromeDriverVersion;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-03-25 5:47 AM
 */
class WebDriverManager extends com.amzass.service.common.WebDriverManager {

    private static final int FACTOR_TIMEOUT = 10;

    private void setTimeOut(long timeout, WebDriver driver) {
        driver.manage().timeouts()
            .implicitlyWait(timeout, TimeUnit.MILLISECONDS)
            .pageLoadTimeout(timeout * FACTOR_TIMEOUT, TimeUnit.MILLISECONDS)
            .setScriptTimeout(timeout * FACTOR_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    @Override
    public WebDriver initCustomChromeDriver(ChromeDriverVersion chromeDriverVersion, long timeout, DesiredCapabilities dCaps) {
        WebDriver driver = super.initCustomChromeDriver(chromeDriverVersion, timeout, dCaps);
        this.setTimeOut(timeout, driver);
        return driver;
    }
}
