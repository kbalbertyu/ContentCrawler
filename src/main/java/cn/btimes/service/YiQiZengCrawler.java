package cn.btimes.service;

import cn.btimes.model.common.BTExceptions.PageEndException;
import cn.btimes.model.common.Config;
import cn.btimes.model.yiqizeng.Product;
import cn.btimes.utils.PageUtils;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.PageLoadHelper;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Constants;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.amzass.utils.common.RegexUtils;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/6/17 23:07
 */
public class YiQiZengCrawler implements ServiceExecutorInterface {
    private static final String DRIVER_KEY = "YiQiZeng";
    private static final String LIST_URL = "/goodsPurchase/toGoodsPurchase?menuID=66";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Inject private WebDriverLauncher webDriverLauncher;

    @Override
    public void execute(Config config) {
        WebDriver driver = webDriverLauncher.startWithoutLogin(DRIVER_KEY);
        this.login(driver, config);

        if (this.accessList(driver, config) && this.accessList(driver, config)) {
            throw new BusinessException("Unable to access to the list page.");
        }

        List<Product> products = this.fetchProducts(driver);
        if (products.size() == 0) {
            logger.warn("No products found.");
            return;
        }
        try {
            this.fetchDetails(driver, config, products);
        } catch (BusinessException e) {
            logger.error("Unable to fetch product details: ", e);
        }
    }

    private void fetchDetails(WebDriver driver, Config config, List<Product> products) {
        int i = 0;
        for (Product product : products) {
            if (!this.accessDetailPage(driver, config.getAdminUrl() + product.formUrl())) {
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
            }
        }
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


    }

    static float formatPrice(String priceText) {
        String priceTextClean = RegexUtils.getMatched(priceText, "[0-9\\.]+");
        if (StringUtils.isBlank(priceTextClean)) {
            throw new BusinessException(String.format("Unable to format price: %s", priceText));
        }
        return NumberUtils.toFloat(priceTextClean);
    }

    private boolean accessDetailPage(WebDriver driver, String url) {
        driver.get(url);
        WaitTime.Normal.execute();
        if (!PageLoadHelper.present(driver, By.id("goodName"), WaitTime.Normal)) {
            driver.get(url);
            WaitTime.Long.execute();
            return PageLoadHelper.present(driver, By.id("goodName"), WaitTime.Normal);
        }
        return true;
    }

    private List<Product> fetchProducts(WebDriver driver) {
        List<Product> products = new ArrayList<>();
        Document doc = Jsoup.parse(driver.getPageSource());
        Element nextElm = doc.select("ul.pagination > li.next").first();
        do {
            try {
                this.parseProductList(doc, products);
                WaitTime.Normal.execute();
                this.nextPage(driver);
            } catch (PageEndException e) {
                logger.warn("Page end: ", e);
                break;
            }
        } while (!nextElm.hasClass("disabled"));

        return products;
    }

    private int parseCurrentPageNo(Document doc) {
        String pageText = HtmlParser.text(doc, "ul.pagination > li.active");
        return NumberUtils.toInt(pageText);
    }

    private void nextPage(WebDriver driver) {
        Document doc = Jsoup.parse(driver.getPageSource());

        int currentPage = this.parseCurrentPageNo(doc);
        PageUtils.click(driver, By.className("next"));
        WaitTime.Long.execute();

        doc = Jsoup.parse(driver.getPageSource());
        int newPage = this.parseCurrentPageNo(doc);

        if (currentPage == newPage) {
            throw new PageEndException(String.format("List page ends on: %s", currentPage));
        }
    }

    private void parseProductList(Document doc, List<Product> products) {
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
            product.setId(NumberUtils.toInt(params.getOrDefault("groupId", "0")));
            product.setGroupType(NumberUtils.toInt(params.getOrDefault("groupType", "0")));
            products.add(product);
        }
    }

    private boolean accessList(WebDriver driver, Config config) {
        driver.get(config.getAdminUrl() + LIST_URL);
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
