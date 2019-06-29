package cn.btimes.service;

import cn.btimes.model.common.BTExceptions.PageEndException;
import cn.btimes.model.common.Config;
import cn.btimes.model.yiqizeng.Product;
import cn.btimes.utils.Common;
import cn.btimes.utils.PageUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.amzass.model.submit.OrderEnums.ReturnCode;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.PageLoadHelper;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.*;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.google.inject.Inject;
import com.mailman.model.common.WebApiResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static cn.btimes.service.WebDriverLauncher.DOWNLOAD_PATH;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/6/17 23:07
 */
public class YiQiZengCrawler implements ServiceExecutorInterface {
    private static final String DRIVER_KEY = "YiQiZeng";
    private static final String DETAIL_DRIVER_KEY = "YiQiZeng_Detail";
    private static final String LIST_URL = "/goodsPurchase/toGoodsPurchase?menuID=66";
    private static final int MIN_DAYS_DIFF_FETCH = 5;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Inject private WebDriverLauncher webDriverLauncher;
    @Inject private ApiRequest apiRequest;

    @Override
    public void execute(Config config) {
        WebDriver driver = webDriverLauncher.startWithoutLogin(DRIVER_KEY);
        WebDriver driverDetail = webDriverLauncher.startWithoutLogin(DETAIL_DRIVER_KEY);
        if (this.accessList(driver, config) && this.accessList(driver, config)) {
            throw new BusinessException("Unable to access to the list page.");
        }

        for (int page = 1; ; page++) {
            logger.info("Fetching from page: {}", page);
            List<Product> products;
            try {
                // this.accessList(driver, config);
                products = this.fetchProducts(driver, page);
            } catch (PageEndException e) {
                logger.warn("Page ended on: {}", page);
                break;
            }
            int size = products.size();
            if (size == 0) {
                logger.warn("No products found.");
                return;
            } else {
                logger.info("Found {} products.", size);
            }
            try {
                this.fetchDetails(driverDetail, config, products);
            } catch (BusinessException e) {
                logger.error("Unable to fetch product details: ", e);
            }
            WaitTime.Normal.execute();
        }
    }

    private void fetchDetails(WebDriver driver, Config config, List<Product> products) {
        int i = 0;
        for (Product product : products) {
            logger.info("Fetching product: {}", product.getId());
            if (this.fetchAlready(product.getId(), config)) {
                logger.info("Product fetched already: {}", product.getId());
                continue;
            }
            if (!this.accessDetailPage(driver, config, product)) {
                logger.error("Unable to open page: {}", product.formUrl());
                continue;
            }
            Document doc = Jsoup.parse(driver.getPageSource());
            try {
                this.parseProductDetail(doc, product);
            } catch (BusinessException e) {
                if (++i >= Constants.MAX_REPEAT_TIMES) {
                    throw e;
                }
                continue;
            }
            this.saveProduct(product, config);
            WaitTime.Normal.execute();
        }
    }

    private boolean fetchAlready(int id, Config config) {
        WebApiResult result = apiRequest.get(config.getFrontUrl() + "/product/fetch/" + id);
        if (result == null || StringUtils.isBlank(result.getData())) {
            return false;
        }
        try {
            Product product = JSONObject.parseObject(result.getData(), Product.class);
            return DateHelper.daysBetween(product.getDateUpdated(), new Date()) <= MIN_DAYS_DIFF_FETCH;
        } catch (JSONException e) {
            logger.error("Unable to parse JSON: {}", result.getData(), e);
            return false;
        }
    }

    private void saveProduct(Product product, Config config) {
        this.downloadImages(product);
        Connection conn = Jsoup.connect(config.getFrontUrl() + config.getArticleSaveUrl())
            .validateTLSCertificates(false)
            .method(Method.POST)
            .data("data", JSON.toJSONString(product));

        int i = 0;
        for (File file : product.getGalleryFiles()) {
            String filePath = file.getAbsolutePath();
            try {
                FileInputStream fs = new FileInputStream(file);
                conn.data("gallery[" + i + "]", filePath, fs);
                i++;
            } catch (IOException e) {
                String message = String.format("Unable to upload file: %s", filePath);
                logger.error(message, e);
                throw new BusinessException(e);
            }
        }

        int j = 0;
        for (File file : product.getContentGalleryFiles()) {
            String filePath = file.getAbsolutePath();
            try {
                FileInputStream fs = new FileInputStream(file);
                conn.data("contentGallery[" + j + "]", filePath, fs);
                i++;
            } catch (IOException e) {
                String message = String.format("Unable to upload file: %s", filePath);
                logger.error(message, e);
                throw new BusinessException(e);
            }
        }

        for (int k = 0; k < Constants.MAX_REPEAT_TIMES; k++) {
            String message;
            try {
                String body = conn.ignoreContentType(true).timeout(0).execute().body();
                WebApiResult result = JSONObject.parseObject(body, WebApiResult.class);
                if (ReturnCode.notFail(result.getCode())) {
                    logger.info("Product saved: {} -> {}", product.getId(), product.getTitle());
                } else {
                    logger.error("Unable to save the product: {} -> {}, error = {}",
                        product.getId(), product.getTitle(), result.getMessage());
                }
                this.deleteFiles(product);
                return;
            } catch (Exception e) {
                message = String.format("Unable to save product, retry in %d seconds:", WaitTime.Normal.val());
                logger.error(message, e);
            }
            WaitTime.Normal.execute();
        }
    }

    private void deleteFiles(Product product) {
        for (File file : product.getGalleryFiles()) {
            FileUtils.deleteQuietly(file);
        }
        for (File file : product.getContentGalleryFiles()) {
            FileUtils.deleteQuietly(file);
        }
    }

    private void downloadImages(Product product) {
        if (product.getGallery().size() > 0) {
            List<File> galleryFiles = new ArrayList<>();
            for (String src : product.getGallery()) {
                File file = this.downloadImage(src, product.getId());
                galleryFiles.add(file);
            }
            product.setGalleryFiles(galleryFiles);
        }
        if (product.getContentGallery().size() > 0) {
            List<File> galleryFiles = new ArrayList<>();
            for (String src : product.getContentGallery()) {
                File file = this.downloadImage(src, product.getId());
                galleryFiles.add(file);
            }
            product.setContentGalleryFiles(galleryFiles);
        }
    }

    private File downloadImage(String url, int productId) {
        String fileName = Common.extractFileNameFromUrl(url);
        File file = this.makeDownloadFile(fileName, String.valueOf(productId));

        String encodedUrl = StringUtils.replace(url, " ", "%20");
        HttpGet get = HttpUtils.prepareHttpGet(encodedUrl);
        CloseableHttpClient httpClient = HttpClients.createDefault();

        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            CloseableHttpResponse resp = null;
            InputStream is = null;
            try {
                resp = httpClient.execute(get);
                int status = resp.getStatusLine().getStatusCode();
                if (status == HttpStatus.SC_OK) {
                    is = resp.getEntity().getContent();
                    FileUtils.copyInputStreamToFile(is, file);
                    return file;
                }
                String message = String.format("Failed to execute file download request: fileName=%s, url=%s, status=%s.", fileName, url, status);
                logger.error(message);
            } catch (Exception ex) {
                String message = String.format("Failed to download file of %s： %s", fileName, Tools.getExceptionMsg(ex));
                logger.error(message);
            } finally {
                get.releaseConnection();
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(resp);
            }
        }
        throw new BusinessException(String.format("Failed to execute %s file download request after retried.", fileName));
    }

    private File makeDownloadFile(String fileName, String productId) {
        String ext = Common.getFileExtension(fileName);
        fileName = Common.toMD5(fileName) + "." + ext;

        File file = FileUtils.getFile(DOWNLOAD_PATH, productId, fileName);
        if (file.exists()) {
            FileUtils.deleteQuietly(file);
            file = FileUtils.getFile(DOWNLOAD_PATH, productId, fileName);
        }
        if (!file.canWrite()) {
            file.setWritable(true);
        }
        return file;
    }

    private void parseProductDetail(Document doc, Product product) {
        Elements titleELms = doc.select("#goodName");
        if (titleELms.size() == 0) {
            throw new BusinessException("Title not found.");
        }
        Element titleElm = titleELms.first();
        product.setTitle(StringUtils.trim(titleElm.text()));

        Element titleNoteElm = titleElm.nextElementSibling();
        if (titleNoteElm != null) {
            product.setTitleNote(StringUtils.trim(titleNoteElm.text()));
        }

        // Prices
        String priceText = HtmlParser.text(doc, ".stock > .stock-top > .price");
        if (StringUtils.isBlank(priceText)) {
            throw new BusinessException("Price not found.");
        }
        product.setStandardPrice(formatPrice(priceText));

        String complexPriceText = HtmlParser.text(doc, ".stock > .stock-bottom");
        String[] prices = StringUtils.split(complexPriceText, "市价");
        if (prices.length != 2) {
            throw new BusinessException(String.format("Unable to find VIP price and market place: %s", complexPriceText));
        }
        product.setVipPrice(formatPrice(prices[0]));
        product.setMarketPrice(formatPrice(prices[1]));

        // Sold and stock
        String soldAndStockText = HtmlParser.text(doc, ".stock > .stock-top > .work");
        String[] parts = StringUtils.split(soldAndStockText, "库存");
        if (parts.length != 2) {
            throw new BusinessException(String.format("Unable to find sold and stock total: %s", soldAndStockText));
        }
        product.setSoldCount(formatNumber(parts[0]));
        product.setInventory(formatNumber(parts[1]));

        // Gallery images
        List<String> gallery = new ArrayList<>();
        Elements galleryElms = doc.select("#toggleImg img");
        for (Element galleryElm : galleryElms) {
            gallery.add(galleryElm.attr("src"));
        }
        product.setGallery(gallery);

        // Detail images
        List<String> contentGallery = new ArrayList<>();
        Elements contentGalleryElms = doc.select("div.rows:contains(图文详情) img");
        for (Element contentGalleryElm : contentGalleryElms) {
            contentGallery.add(contentGalleryElm.attr("src"));
        }
        product.setContentGallery(contentGallery);

        String note = HtmlParser.text(doc, "div.rows:contains(图文详情)");
        note = StringUtils.trim(StringUtils.remove(note, "图文详情"));
        product.setDescription(note);

        // Product specs
        Map<String, List<String>> specs = new HashMap<>();
        Elements specsElms = doc.select("#spec > div");
        for (Element specsElm : specsElms) {
            String name = HtmlParser.text(specsElm, "p");
            List<String> spec = HtmlParser.texts(specsElm.select(".group-spec > div"));
            specs.put(name, spec);
        }
        product.setSpecs(specs);
    }

    static float formatPrice(String priceText) {
        String priceTextClean = RegexUtils.getMatched(priceText, "[0-9\\.]+");
        if (StringUtils.isBlank(priceTextClean)) {
            throw new BusinessException(String.format("Unable to format price: %s", priceText));
        }
        return NumberUtils.toFloat(priceTextClean);
    }

    private static int formatNumber(String numberText) {
        String numberTextClean = RegexUtils.getMatched(numberText, "[0-9]+");
        if (StringUtils.isBlank(numberTextClean)) {
            throw new BusinessException(String.format("Unable to format number: %s", numberText));
        }
        return NumberUtils.toInt(numberTextClean);
    }

    private boolean accessDetailPage(WebDriver driver, Config config, Product product) {
        String url = config.getAdminUrl() + product.formUrl();
        driver.get(url);
        WaitTime.Short.execute();
        if (!this.loginAlready(driver)) {
            this.login(driver, config);
            driver.get(url);
        }
        WaitTime.Normal.execute();
        if (!PageLoadHelper.present(driver, By.id("goodName"), WaitTime.Normal)) {
            driver.get(url);
            WaitTime.Normal.execute();
            return PageLoadHelper.present(driver, By.id("goodName"), WaitTime.Normal);
        }
        return true;
    }

    private List<Product> fetchProducts(WebDriver driver, int page) {
        this.goToPage(driver, page);
        Document doc = Jsoup.parse(driver.getPageSource());
        return this.parseProductList(doc);
    }

    private void goToPage(WebDriver driver, int toPage) {
        List<WebElement> pageElms = driver.findElements(By.cssSelector(".pagination > .page"));

        WebElement lastShownPage = null;
        for (WebElement pageElm : pageElms) {
            int page = NumberUtils.toInt(pageElm.getText());
            if (page == toPage) {
                PageUtils.click(driver, pageElm.findElement(By.tagName("a")));
                WaitTime.Normal.execute();
                return;
            }
            lastShownPage = pageElm;
        }
        if (StringUtils.contains(lastShownPage.getAttribute("class"), "active")) {
            throw new PageEndException("Page ended, page not found: " + toPage);
        }
        WebElement next = driver.findElement(By.cssSelector(".pagination > .next"));
        if (StringUtils.contains(next.getAttribute("class"), "disabled")) {
            throw new PageEndException("Page ended, page not found: " + toPage);
        }
        PageUtils.click(driver, lastShownPage);
        WaitTime.Normal.execute();
        this.goToPage(driver, toPage);
    }

    private List<Product> parseProductList(Document doc) {
        List<Product> products = new ArrayList<>();
        Elements rows = doc.select("#productGoods > .row > .col-sm-3");
        if (rows.size() == 0) {
            throw new PageEndException("No product found on page");
        }
        for (Element row : rows) {
            Elements linkElms = row.select("a[href*=goodsPurchase/toCommoditydetails]");
            if (linkElms.size() == 0) {
                continue;
            }
            String link = linkElms.first().attr("href");
            Map<String, String> params = PageUtils.getParameters(link);
            Product product = new Product();
            product.setId(NumberUtils.toInt(params.getOrDefault("goodID", "0")));
            product.setGroupType(NumberUtils.toInt(params.getOrDefault("groupType", "0")));
            products.add(product);
        }
        return products;
    }

    private boolean accessList(WebDriver driver, Config config) {
        String url = config.getAdminUrl() + LIST_URL;
        driver.get(url);
        if (!this.loginAlready(driver)) {
            this.login(driver, config);
            driver.get(url);
        }
        return !PageLoadHelper.present(driver, By.id("productGoods"), WaitTime.Long);
    }

    private void login(WebDriver driver, Config config) {
        driver.get(config.getAdminUrl());

        if (loginAlready(driver)) {
            return;
        }

        By captchaBy = By.id("auth_code");
        By passwordBy = By.name("pwd");

        if (!PageLoadHelper.present(driver, captchaBy, WaitTime.Normal) &&
            PageLoadHelper.present(driver, passwordBy, WaitTime.Shortest)) {
            driver.get(config.getAdminUrl());
            if (!PageLoadHelper.present(driver, captchaBy, WaitTime.Long) &&
                PageLoadHelper.present(driver, passwordBy, WaitTime.Shortest)) {
                throw new BusinessException("Unable to find captcha.");
            }
        }

        WebElement captchaElm = driver.findElement(captchaBy);
        String captcha = captchaElm.getText();

        PageUtils.setValue(driver, By.name("name"), config.getAdminEmail());
        PageUtils.setValue(driver, passwordBy, config.getAdminPassword());
        PageUtils.setValue(driver, By.name("verify"), captcha);

        PageUtils.click(driver, By.id("administrate"));

        if (loginAlready(driver)) {
            return;
        }
        throw new BusinessException("Unable to login.");
    }

    private boolean loginAlready(WebDriver driver) {
        return PageLoadHelper.present(driver, By.className("img-circle"), WaitTime.Normal);
    }
}
