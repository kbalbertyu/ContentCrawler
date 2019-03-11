package cn.btimes.service;

import cn.btimes.model.common.Config;
import cn.btimes.ui.ContentCrawler.Application;
import com.amzass.enums.common.ConfigEnums.ChromeDriverVersion;
import com.amzass.service.common.WebDriverManager;
import com.amzass.utils.PageLoadHelper;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Constants;
import com.amzass.utils.common.PageUtils;
import com.amzass.utils.common.Tools;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2018-12-29 2:37 PM
 */
public class WebDriverLauncher {
    static final String DOWNLOAD_PATH = System.getProperty("user.dir") + "/downloads";
    private static final boolean USE_HEADLESS_DRIVER = StringUtils.isNotBlank(Tools.getCustomizingValue("USE_HEADLESS_DRIVER"));
    @Inject private WebDriverManager webDriverManager;
    public static Map<Application, Map<String, String>> adminCookies;

    public WebDriver start(Config config) {
        return this.startDriver(config, true);
    }

    public WebDriver startWithoutLogin(Config config) {
        return this.startDriver(config, false);
    }

    private WebDriver startDriver(Config config, boolean login) {
        ChromeDriverVersion chromeDriverVersion = Tools.defaultChromeDriver();
        if (chromeDriverVersion == null) {
            chromeDriverVersion = ChromeDriverVersion.values()[0];
        }

        DesiredCapabilities dCaps = this.prepareChromeCaps();
        WebDriver driver = webDriverManager.initCustomChromeDriver(chromeDriverVersion, Constants.DEFAULT_DRIVER_TIME_OUT, dCaps);
        if (login && (adminCookies == null || adminCookies.getOrDefault(config.getApplication(), null) == null)) {
            this.fetchAdminCookies(driver, config);
        }
        return driver;
    }

    private DesiredCapabilities prepareChromeCaps() {
        HashMap<String, Object> chromePrefs = new HashMap<>();
        chromePrefs.put("profile.default_content_settings.popups", 0);
        chromePrefs.put("download.default_directory", DOWNLOAD_PATH);
        ChromeOptions options = new ChromeOptions();
        if (USE_HEADLESS_DRIVER) {
            options.addArguments("--headless");
        }
        options.setExperimentalOption("prefs", chromePrefs);
        DesiredCapabilities cap = DesiredCapabilities.chrome();
        cap.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
        cap.setCapability(ChromeOptions.CAPABILITY, options);
        return cap;
    }

    private void fetchAdminCookies(WebDriver driver, Config config) {
        driver.get(config.getAdminUrl());
        PageLoadHelper.visible(driver, By.id("mb_email"), WaitTime.Normal);
        PageUtils.setValue(driver, By.id("mb_email"), config.getAdminEmail());
        PageUtils.setValue(driver, By.id("login_mb_password"), config.getAdminPassword());
        PageUtils.click(driver, By.cssSelector("button[type=submit]"));
        WaitTime.Normal.execute();
        Set<Cookie> cookieSet = driver.manage().getCookies();

        adminCookies = new HashMap<>();
        Map<String, String> adminCookie = new HashMap<>();
        for (Cookie cookie : cookieSet) {
            adminCookie.put(cookie.getName(), cookie.getValue());
        }
        adminCookies.put(config.getApplication(), adminCookie);
    }
}
