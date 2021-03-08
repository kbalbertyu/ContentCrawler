package cn.btimes.service;

import cn.btimes.model.common.Config;
import cn.btimes.model.common.Store;
import cn.btimes.utils.PageUtils;
import com.alibaba.fastjson.JSON;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/10/31 19:22
 */
public class GuoaoCrawler implements ServiceExecutorInterface {
    private final Logger logger = LoggerFactory.getLogger(GuoaoCrawler.class);
    @Inject private WebDriverLauncher webDriverLauncher;
    private static final String SOURCE_URL = "https://aila.site/ams/#/meetingManage/conference";
    private static final String FRONT_URL = "http://guoao2020.com/exhibition/detail?id=18";

    @Override
    public void execute(Config config) {
        WebDriver driver = webDriverLauncher.startWithoutLogin(config.getApplication().name());

        WaitTime.Normal.execute();
        this.crawlMembers(driver);
        System.out.println("Done");
    }

    /**
     * Crawl from frontend
     *
     * @param driver
     */
    private void crawlFromFront(WebDriver driver) {
        driver.get(FRONT_URL);
        WaitTime.Normal.execute();
        List<Store> stores = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            this.crawlFrontPage(driver, stores);
            logger.info("Complete page: {}", i);

            try {
                WebElement nextElm = driver.findElement(By.className("btn-next"));
                PageUtils.click(driver, nextElm);
                WaitTime.Normal.execute();
            } catch (NoSuchElementException e) {
                logger.error("Next button not found: {}", i);
            }
        }

        System.out.println(JSON.toJSONString(stores));
    }

    private void crawlFrontPage(WebDriver driver, List<Store> stores) {
        List<WebElement> elements = driver.findElements(By.className("itemsearch"));
        for (WebElement element : elements) {
            try {
                WebElement gotoElm = element.findElement(By.className("goto"));
                String listHandle = driver.getWindowHandle();
                logger.info("List handle: {}", listHandle);
                PageUtils.click(driver, gotoElm);
                this.switchHandle(driver, listHandle);
                WaitTime.Shortest.execute();

                Store store = new Store();
                By profileBy = By.className("followBut");
                try {
                    List<WebElement> profileElms = driver.findElements(profileBy);
                    WebElement profileElm = null;
                    for (WebElement elm : profileElms) {
                        if (StringUtils.equals(elm.getText(), "企业画像")) {
                            profileElm = elm;
                            break;
                        }
                    }
                    if (profileElm == null) {
                        throw new NoSuchElementException("");
                    }
                    PageUtils.click(driver, profileElm);
                    WaitTime.Shortest.execute();
                    driver.close();
                    this.switchHandle(driver, listHandle);

                    this.parseFromProfilePage(driver, store);
                } catch (NoSuchElementException e) {
                    this.parseFromViewPage(driver, store);
                }

                String currentHandle = driver.getWindowHandle();
                logger.info("Current handle: {}", currentHandle);

                Set<String> handles = driver.getWindowHandles();
                if (handles.size() > 1) {
                    driver.close();
                } else {
                    logger.error("Window tabs error");
                }
                if (store.valid()) {
                    stores.add(store);
                }

                driver.switchTo().window(listHandle);
                WaitTime.Shortest.execute();
            } catch (WebDriverException e) {
                logger.error("Driver error: {}", e.getMessage(), e);
            }
        }
        System.out.println(JSON.toJSONString(stores));
    }

    private void switchHandle(WebDriver driver, String excludedHandle) {
        Set<String> handles = driver.getWindowHandles();
        for (String handle : handles) {
            if (!excludedHandle.equals(handle)) {
                driver.switchTo().window(handle);
                return;
            }
        }
    }

    private void parseFromViewPage(WebDriver driver, Store store) {
        // Gallery
        Document doc = Jsoup.parse(driver.getPageSource());
        Element titleElm = doc.select(".companyTitle > span.fl").first();
        if (titleElm == null) {
            return;
        }
        store.setName(StringUtils.trim(titleElm.text()));
        Elements imageElms = doc.select(".el-carousel__item");
        List<String> gallery = new ArrayList<>();
        for (Element element : imageElms) {
            String src = element.select("img").get(0).attr("src");
            gallery.add(src);
        }
        store.setGallery(gallery);

        // Video
        this.parseVideos(driver, store);
    }

    private void parseVideos(WebDriver driver, Store store) {
        try {
            List<String> videos = new ArrayList<>();
            By by = By.className("el-icon-video-play");
            List<WebElement> elements = driver.findElements(by);

            int i = 0;
            for (WebElement element : elements) {
                if (i % 3 == 0 && i != 0) {
                    PageUtils.click(driver, By.cssSelector(".videoSilder .el-carousel__arrow--right"));
                    WaitTime.Shortest.execute();
                }
                i++;
                PageUtils.click(driver, element);
                WaitTime.Short.execute();
                Document doc = Jsoup.parse(driver.getPageSource());
                Element videoElm = doc.select("video").first();
                if (videoElm == null) continue;
                videos.add(videoElm.attr("src"));
                PageUtils.click(driver, By.className("closeDialog"));
                WaitTime.Shortest.execute();
            }
            store.setVideos(videos);
        } catch (NoSuchElementException e) {
            logger.error("No found element: ", e);
        }
    }

    private void parseFromProfilePage(WebDriver driver, Store store) {
        Document doc = Jsoup.parse(driver.getPageSource());
        Element titleElm = doc.select(".companyTitle > span.fl").first();
        if (titleElm == null) {
            return;
        }
        store.setName(StringUtils.trim(titleElm.text()));

        // Certificates
        List<String> certificates = new ArrayList<>();
        Elements certElms = doc.select(".certificate .el-image");
        for (Element certElm : certElms) {
            String src = certElm.select("img").first().attr("src");
            certificates.add(src);
        }
        store.setCertificates(certificates);

        // Gallery
        List<String> gallery = new ArrayList<>();
        Elements galleryElms = doc.select(".about_image .el-image");
        for (Element galleryElm : galleryElms) {
            String src = galleryElm.select("img").first().attr("src");
            gallery.add(src);
        }
        store.setGallery(gallery);

        // Video
        this.parseVideos(driver, store);
    }

    /**
     * Crawl from backend
     *
     * @param driver
     */
    private void crawlMembers(WebDriver driver) {
        driver.get(SOURCE_URL);
        WaitTime.Normal.execute();
        PageUtils.click(driver, By.id("tab-second"));
        List<Map<String, String>> items = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            this.crawlMemberPage(driver, items);
            logger.info("Complete page: {}", i);
            PageUtils.click(driver, By.cssSelector("#pane-second .btn-next"));
            WaitTime.Normal.execute();
        }
        logger.info(JSON.toJSONString(items));
    }

    private void crawlMemberPage(WebDriver driver, List<Map<String, String>> items) {
        List<WebElement> elements = driver.findElements(By.cssSelector("#pane-second > .table-content .el-table__body > tbody > tr > td:nth-child(2) > .el-tooltip > .clickStyle"));

        int i = 0;
        for (WebElement element : elements) {
            logger.info("Member: {}. {}", i++, element.getText());
            PageUtils.click(driver, element);
            WaitTime.Normal.execute();

            Document doc = Jsoup.parse(driver.getPageSource());
            Map<String, String> item = this.parseMemberInfo(doc);

            PageUtils.click(driver, By.cssSelector("div[role=dialog][aria-label=账号详情] button[aria-label=Close]"));
            if (item == null) {
                continue;
            }
            items.add(item);
            WaitTime.Short.execute();
        }
    }

    private Map<String, String> parseMemberInfo(Document doc) {
        Element dialog = doc.select("div.el-dialog[aria-label=账号详情]").first();
        if (dialog == null) {
            return null;
        }
        Elements elements = dialog.select(".nameInfo > .item, .contactInfo > .item");
        Map<String, String> map = this.parseKeyValue(elements);

        Element avatar = dialog.select(".avatar > img").first();
        if (avatar != null) {
            String avatarSrc = avatar.attr("src");
            if (StringUtils.containsIgnoreCase(avatarSrc, "http")) {
                map.put("avatar", avatarSrc);
            }
        }

        return map;
    }

    private Map<String, String> parseKeyValue(Elements elements) {
        Map<String, String> map = new HashMap<>();
        for (Element element : elements) {
            String key = HtmlParser.text(element, ".key").trim();
            key = StringUtils.remove(key, "：");
            String value = HtmlParser.text(element, ".value").trim();
            if (StringUtils.equals(value, "-")) {
                value = "";
            }
            if (StringUtils.isBlank(value)) {
                continue;
            }
            map.put(key, value);
        }
        if (StringUtils.isBlank(map.get("手机号")) && StringUtils.isBlank(map.get("邮箱"))) {
            // return null;
        }
        return map;
    }

    /**
     * Crawl from backend
     *
     * @param driver
     */
    private void crawlListInfo(WebDriver driver) {
        driver.get(SOURCE_URL);
        List<Map<String, String>> items = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            this.crawlPage(driver, items);
            logger.info("Complete page: {}", i);
            PageUtils.click(driver, By.className("btn-next"));
            WaitTime.Normal.execute();
        }
        logger.info(JSON.toJSONString(items));
    }

    private void crawlPage(WebDriver driver, List<Map<String, String>> items) {
        List<WebElement> elements = driver.findElements(By.cssSelector(".el-table__body-wrapper > table.el-table__body > tbody > tr > td:nth-child(3) span"));

        int i = 0;
        for (WebElement element : elements) {
            logger.info("Item: {}. {}", i++, element.getText());
            PageUtils.click(driver, element);
            WaitTime.Short.execute();

            Document doc = Jsoup.parse(driver.getPageSource());
            Map<String, String> item = this.parse(doc);

            PageUtils.click(driver, By.cssSelector("div[role=dialog][aria-label=公司详情] button[aria-label=Close]"));
            if (item == null) {
                continue;
            }
            items.add(item);
        }
    }

    private Map<String, String> parse(Document doc) {
        Element dialog = doc.select("div[role=dialog][aria-label=公司详情]").first();
        if (dialog == null) {
            return null;
        }
        Elements elements = dialog.select(".item, .itemInner");
        Map<String, String> map = this.parseKeyValue(elements);

        Element logo = dialog.select(".logo > img").first();
        if (logo != null) {
            String logoSrc = logo.attr("src");
            if (StringUtils.containsIgnoreCase(logoSrc, "http")) {
                map.put("logo", logoSrc);
            }
        } else {
            logger.error("Logo not found");
        }

        return map;
    }
}
