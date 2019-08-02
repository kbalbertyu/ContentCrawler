package cn.btimes.service;

import cn.btimes.model.common.Application;
import cn.btimes.model.common.Config;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.amzass.enums.common.ConfigEnums.ChromeDriverVersion;
import com.amzass.enums.common.Directory;
import com.amzass.utils.PageLoadHelper;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Constants;
import com.amzass.utils.common.PageUtils;
import com.amzass.utils.common.Tools;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2018-12-29 2:37 PM
 */
public class WebDriverLauncher {
    static final String DOWNLOAD_PATH = System.getProperty("user.dir") + "/downloads";
    private static final boolean USE_HEADLESS_DRIVER = StringUtils.isNotBlank(Tools.getCustomizingValue("USE_HEADLESS_DRIVER"));
    @Inject private WebDriverManager webDriverManager;
    public static Map<Application, Map<String, String>> adminCookies;

    public WebDriver start(Config config) {
        return this.startDriver(config, true, null);
    }

    public WebDriver startWithoutLogin(String profile) {
        return this.startDriver(null, false, profile);
    }

    private WebDriver startDriver(Config config, boolean login, String profile) {
        ChromeDriverVersion chromeDriverVersion = Tools.defaultChromeDriver();
        if (chromeDriverVersion == null) {
            chromeDriverVersion = ChromeDriverVersion.values()[0];
        }

        DesiredCapabilities dCaps = this.prepareChromeCaps(profile);
        WebDriver driver = webDriverManager.initCustomChromeDriver(chromeDriverVersion, Constants.DEFAULT_DRIVER_TIME_OUT, dCaps);
        if (login && config != null && (adminCookies == null || adminCookies.getOrDefault(config.getApplication(), null) == null)) {
            this.fetchAdminCookies(driver, config);
        }
        return driver;
    }

    private DesiredCapabilities prepareChromeCaps(String profile) {
        HashMap<String, Object> chromePrefs = new HashMap<>();
        chromePrefs.put("profile.default_content_settings.popups", 0);
        chromePrefs.put("download.default_directory", DOWNLOAD_PATH);
        ChromeOptions options = new ChromeOptions();
        if (USE_HEADLESS_DRIVER) {
            options.addArguments("--headless");
        }
        options.setExperimentalOption("prefs", chromePrefs);
        if (StringUtils.isNotBlank(profile)) {
            options.addArguments("user-data-dir=Profile\\" + profile);
        }

        DesiredCapabilities cap = DesiredCapabilities.chrome();
        cap.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
        cap.setCapability(ChromeOptions.CAPABILITY, options);
        return cap;
    }

    private void fetchAdminCookies(WebDriver driver, Config config) {
        driver.get(config.getAdminUrl());

        File cookieFile = this.getCookieFile(config);
        if (!PageLoadHelper.visible(driver, By.id("mb_email"), WaitTime.Normal)) {
            this.loadCookies(driver, config, cookieFile);
            return;
        }

        if (cookieFile.exists()) {
            Map<String, String> cookies = JSON.parseObject(Tools.readFileToString(cookieFile), new TypeReference<Map<String, String>>() {
            });
            PageUtils.addCookies(driver, cookies);
            driver.get(config.getAdminUrl());
        }

        if (!PageLoadHelper.visible(driver, By.id("mb_email"), WaitTime.Normal)) {
            this.loadCookies(driver, config, cookieFile);
            return;
        }
        driver.manage().deleteAllCookies();

        PageUtils.setValue(driver, By.id("mb_email"), config.getAdminEmail());
        PageUtils.setValue(driver, By.id("login_mb_password"), config.getAdminPassword());
        PageUtils.click(driver, By.cssSelector("button[type=submit]"));
        WaitTime.Normal.execute();

        this.loadCookies(driver, config, cookieFile);
    }

    private void loadCookies(WebDriver driver, Config config, File cookieFile) {
        Map<String, String> adminCookie = PageUtils.getCookies(driver);
        adminCookies = new HashMap<>();
        adminCookies.put(config.getApplication(), adminCookie);
        Tools.writeStringToFile(cookieFile, JSON.toJSONString(adminCookie, true));
    }

    private File getCookieFile(Config config) {
        String cookieFileName = String.format("ASC-Cookies/%s.json", config.getApplication());
        return new File(Directory.Tmp.path(), cookieFileName);
    }
}
