package cn.btimes.service;

import cn.btimes.model.common.Application;
import cn.btimes.model.common.Config;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.amzass.enums.common.ConfigEnums.ChromeDriverVersion;
import com.amzass.enums.common.Directory;
import com.amzass.service.ocr.CaptchaResolver;
import com.amzass.service.ocr.CaptchaResolver.Handler;
import com.amzass.utils.PageLoadHelper;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Constants;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.amzass.utils.common.PageUtils;
import com.amzass.utils.common.Tools;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2018-12-29 2:37 PM
 */
public class WebDriverLauncher {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    static final String DOWNLOAD_PATH = System.getProperty("user.dir") + "/downloads";
    private static final boolean USE_HEADLESS_DRIVER = StringUtils.isNotBlank(Tools.getCustomizingValue("USE_HEADLESS_DRIVER"));
    private static final boolean DISABLE_JS_IMG_ADS = false;
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
        if (DISABLE_JS_IMG_ADS) {
            chromePrefs.put("profile.managed_default_content_settings.images", 2);
            chromePrefs.put("profile.managed_default_content_settings.javascript", 2);
            chromePrefs.put("profile.managed_default_content_settings.ads", 2);
        }

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

        try {
            PageUtils.click(driver, By.cssSelector("button[type=submit]"));
            WaitTime.Normal.execute();
            this.loadCookies(driver, config, cookieFile);
        } catch (UnhandledAlertException e) {
            this.resetPassword(driver, config);
            this.loadCookies(driver, config, cookieFile);
        }
    }

    @Inject private CaptchaResolver captchaResolver;

    private void resetPassword(WebDriver driver, Config config) {
        Alert alert = driver.switchTo().alert();
        if (alert != null) {
            alert.accept();
        }
        if (!PageLoadHelper.present(driver, By.name("mb_password"), WaitTime.Short)) {
            return;
        }
        PageUtils.setValue(driver, By.name("mb_password"), config.getAdminPassword());
        PageUtils.setValue(driver, By.name("mb_password_re"), config.getAdminPassword());
        String code = this.resolveCaptcha(config);
        if (StringUtils.isNotBlank(code)) {
            PageUtils.setValue(driver, By.name("wr_key"), code);
        }
        PageUtils.click(driver, By.cssSelector("button[type=submit]"));
        WaitTime.Short.execute();
    }

    private String resolveCaptcha(Config config) {
        String captchaSrc = config.getAdminUrl() + "/plugin/captcha/";
        Handler[] handlers = captchaResolver.getHandlers();
        String code = null;
        for (Handler handler : handlers) {
            try {
                code = handler.process(captchaSrc, null);
                if (StringUtils.isNotBlank(code)) {
                    break;
                }
            } catch (WebDriverException e) {
                // 如果是WebDriver异常, 重复处理实际无效, 终止
                logger.error("通过{}处理验证码过程中出现终结性WebDriver异常:", handler.id(), e);
                break;
            } catch (Exception e) {
                logger.error("尝试通过{}处理验证码过程中出现异常:", handler.id(), e);
            }
            WaitTime.Normal.execute();
        }
        if (StringUtils.isBlank(code)) {
            throw new BusinessException("Unable to parse captcha code.");
        }
        return code;
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
