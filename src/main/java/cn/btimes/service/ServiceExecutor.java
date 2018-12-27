package cn.btimes.service;

import cn.btimes.source.Source;
import cn.btimes.source.ThePaper;
import com.amzass.enums.common.ConfigEnums.ChromeDriverVersion;
import com.amzass.service.common.ApplicationContext;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 12/24/2018 8:33 AM
 */
public class ServiceExecutor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String DOWNLOAD_PATH = System.getProperty("user.dir") + "/downloads";
    @Inject WebDriverManager webDriverManager;

    private final static List<Source> sources = new ArrayList<>();
    static {
        sources.add(ApplicationContext.getBean(ThePaper.class));
    }
    public void execute() {
        WebDriver driver = this.launchWebDriver();
        Map<String, String> cookies = this.fetchAdminCookies(driver);
        for (Source source : sources) {
            try {
                source.initAdminCookies(cookies);
                source.execute(driver);
            } catch (Exception e) {
                logger.error("Error found in executing: " + this.getClass(), e);
            }
        }
        driver.close();
    }

    private Map<String,String> fetchAdminCookies(WebDriver driver) {
        driver.get(Source.ADMIN_URL);
        String email = Tools.getCustomizingValue("ADMIN_EMAIL");
        String password = Tools.getCustomizingValue("ADMIN_PASSWORD");
        PageLoadHelper.visible(driver, By.id("mb_email"), WaitTime.Normal);
        PageUtils.setValue(driver, By.id("mb_email"), email);
        PageUtils.setValue(driver, By.id("login_mb_password"), password);
        PageUtils.click(driver, By.cssSelector("button[type=submit]"));
        WaitTime.Normal.execute();
        Set<Cookie> cookieSet = driver.manage().getCookies();

        Map<String, String> cookies = new HashMap<>();
        for (Cookie cookie : cookieSet) {
            cookies.put(cookie.getName(), cookie.getValue());
        }
        return cookies;
    }

    private WebDriver launchWebDriver() {
        ChromeDriverVersion chromeDriverVersion = Tools.defaultChromeDriver();
        if (chromeDriverVersion == null) {
            chromeDriverVersion = ChromeDriverVersion.values()[0];
        }

        DesiredCapabilities dCaps = this.prepareChromeCaps();
        return webDriverManager.initCustomChromeDriver(chromeDriverVersion, Constants.DEFAULT_DRIVER_TIME_OUT, dCaps);
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
}
