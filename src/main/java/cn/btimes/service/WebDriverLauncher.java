package cn.btimes.service;

import cn.btimes.source.Source;
import com.amzass.enums.common.ConfigEnums.ChromeDriverVersion;
import com.amzass.service.common.WebDriverManager;
import com.amzass.utils.PageLoadHelper;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Constants;
import com.amzass.utils.common.PageUtils;
import com.amzass.utils.common.Tools;
import com.google.inject.Inject;
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
    private static final String DOWNLOAD_PATH = System.getProperty("user.dir") + "/downloads";
    @Inject private WebDriverManager webDriverManager;
    public static Map<String, String> adminCookies;

    public WebDriver start() {
        ChromeDriverVersion chromeDriverVersion = Tools.defaultChromeDriver();
        if (chromeDriverVersion == null) {
            chromeDriverVersion = ChromeDriverVersion.values()[0];
        }

        DesiredCapabilities dCaps = this.prepareChromeCaps();
        WebDriver driver = webDriverManager.initCustomChromeDriver(chromeDriverVersion, Constants.DEFAULT_DRIVER_TIME_OUT, dCaps);
        this.fetchAdminCookies(driver);
        return driver;
    }

    private DesiredCapabilities prepareChromeCaps() {
        HashMap<String, Object> chromePrefs = new HashMap<>();
        chromePrefs.put("profile.default_content_settings.popups", 0);
        chromePrefs.put("download.default_directory", DOWNLOAD_PATH);
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", chromePrefs);
        DesiredCapabilities cap = DesiredCapabilities.chrome();
        cap.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
        cap.setCapability(ChromeOptions.CAPABILITY, options);
        return cap;
    }

    private void fetchAdminCookies(WebDriver driver) {
        driver.get(Source.ADMIN_URL);
        String email = Tools.getCustomizingValue("ADMIN_EMAIL");
        String password = Tools.getCustomizingValue("ADMIN_PASSWORD");
        PageLoadHelper.visible(driver, By.id("mb_email"), WaitTime.Normal);
        PageUtils.setValue(driver, By.id("mb_email"), email);
        PageUtils.setValue(driver, By.id("login_mb_password"), password);
        PageUtils.click(driver, By.cssSelector("button[type=submit]"));
        WaitTime.Normal.execute();
        Set<Cookie> cookieSet = driver.manage().getCookies();

        adminCookies = new HashMap<>();
        for (Cookie cookie : cookieSet) {
            adminCookies.put(cookie.getName(), cookie.getValue());
        }
    }
}
