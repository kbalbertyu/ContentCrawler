package com.fortis.service;

import cn.btimes.model.common.Config;
import cn.btimes.service.ServiceExecutorInterface;
import cn.btimes.service.WebDriverLauncher;
import cn.btimes.utils.Common;
import cn.btimes.utils.PageUtils;
import com.amzass.enums.common.Country;
import com.amzass.enums.common.Directory;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.ui.utils.UITools;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.amzass.utils.common.Exceptions.RobotFoundException;
import com.amzass.utils.common.RegexUtils.Regex;
import com.amzass.utils.common.Tools;
import com.fortis.model.AmzHot;
import com.fortis.model.Product;
import com.google.inject.Inject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/8/8 9:06
 */
public class AmazonCrawler implements ServiceExecutorInterface {
    private static final int MAX_REVIEWS = 150;
    private static final float MIN_PRICE = 10f;
    private static final String LINE_SEPARATOR = "----------------";
    private final Logger logger = LoggerFactory.getLogger(AmazonCrawler.class);
    private static final String[] EXCLUDED_CATEGORIES = {
        "Amazon Devices & Accessories",
        "Amazon Launchpad",
        "Appliances",
        "Apps & Games",
        "Arts, Crafts & Sewing",
        "Audible Books & Originals",
        "Automotive",
        "Books",
        "CDs & Vinyl",
        "Camera & Photo",
        "Cell Phones & Accessories",
        "Clothing, Shoes & Jewelry",
        "Collectible Currencies",
        "Computers & Accessories",
        "Digital Music",
        "Electronics",
        "Entertainment Collectibles",
        "Grocery & Gourmet Food",
        "Handmade Products",
        "Kindle Store",
        "Movies & TV",
        "Patio, Lawn & Garden",
        "Prime Pantry",
        "Toys & Games",
        "Video Games",
        "Gift Cards",
        "Software",
        "Sports Collectibles",
        "Magazine Subscriptions",
        "Smart Home"
    };

    @Inject private WebDriverLauncher webDriverLauncher;
    private WebDriver driver;
    private Set<String> asins = new HashSet<>();

    @Override
    public void execute(Config config) {
        this.crawlFromHotLists();
    }

    /**
     * Crawl from: Best Sellers, New Releases, Movers & Shakers, Most Wished For
     * Requires: price(>= MIN_PRICE), stars(< MAX_STARS), reviews(< MIN_PRICE)
     */
    private void crawlFromHotLists() {
        driver = webDriverLauncher.startWithoutLogin(this.getClass().getSimpleName());
        Country country = Country.US;
        for (AmzHot amzHot : AmzHot.values()) {
            Map<AmzHot, Set<Product>> products = new HashMap<>();
            logger.info("Crawling from: {}", amzHot.name());
            try {
                Set<Product> list = this.crawlFromHotList(country, amzHot);
                if (CollectionUtils.isEmpty(list)) {
                    logger.warn("No product found from {}", amzHot.name());
                    continue;
                }
                logger.info("Found {} products from {}", list.size(), amzHot.name());
                products.put(amzHot, list);
            } catch (BusinessException e) {
                logger.error("Unknown exception found in fetching from {}: ", amzHot.name(), e);
            } catch (RobotFoundException e) {
                logger.error("Skip crawling due to captcha detected: ", e);
                break;
            } finally {
                this.saveLinks(products, amzHot);
            }
        }
    }

    private void saveLinks(Map<AmzHot, Set<Product>> products, AmzHot amzHot) {
        if (products.size() == 0) {
            logger.warn("No products found.");
            return;
        }
        String date = DateFormatUtils.format(new Date(), "yyyy-MM-dd-HH-mm");
        File file = FileUtils.getFile(Directory.Tmp.path(), "amzHot-" + amzHot.name() + "-" + date + ".md");

        StringBuilder sb = new StringBuilder();
        for (AmzHot key : products.keySet()) {
            sb.append("# ").append(key.name())
                .append(StringUtils.LF);

            Map<String, Set<Product>> productMap = this.mapProductByCategory(products, key);

            for (String category : productMap.keySet()) {
                sb.append(category)
                    .append(StringUtils.LF)
                    .append(LINE_SEPARATOR)
                    .append(StringUtils.LF);
                for (Product product : productMap.get(category)) {
                    sb.append("+ ")
                        .append("[").append(product.getTitle()).append("]")
                        .append("(").append(product.getUrl()).append(")")
                        .append(StringUtils.LF)
                        .append(StringUtils.LF)
                        .append("```")
                        .append(StringUtils.LF)
                        .append(StringUtils.LF)
                        .append(String.format("Stars=%s, Reviews=%s, Price=%s", product.getStars(), product.getReviews(), product.getPrice()))
                        .append(StringUtils.LF)
                        .append(StringUtils.LF)
                        .append("```")
                        .append(StringUtils.LF)
                        .append(StringUtils.LF)
                        .append("![](").append(product.getImage()).append(")")
                        .append(StringUtils.LF)
                        .append(StringUtils.LF);
                }
                sb.append(StringUtils.LF);
            }
            sb.append(StringUtils.LF);
        }
        Tools.writeStringToFile(file, sb.toString());
        logger.info("Urls saved to file: {}", file.getAbsolutePath());
    }

    private Map<String, Set<Product>> mapProductByCategory(Map<AmzHot, Set<Product>> products, AmzHot key) {
        Map<String, Set<Product>> productMap = new HashMap<>();
        for (Product product : products.get(key)) {
            String category = product.getCategory();
            Set<Product> map = productMap.getOrDefault(category, new HashSet<>());
            map.add(product);
            productMap.put(category, map);
        }
        return productMap;
    }

    private Set<Product> crawlFromHotList(Country country, AmzHot amzHot) {
        String url = country.securedBaseUrl() + amzHot.path;

        Map<String, String> pageLinks = new HashMap<>();
        this.fetchPageLinks(url, pageLinks, null);

        if (pageLinks.size() == 0) {
            logger.error("Unable to find category page links");
            return null;
        }
        this.fetchSubPageLinks(pageLinks);
        // Fetch product links from category pages
        return this.fetchProductsFromCategories(pageLinks);
    }

    private Document getDocument(String url) {
        driver.get(url);
        WaitTime.Normal.execute();
        return Jsoup.parse(driver.getPageSource());
    }

    /**
     * Fetch page links of categories
     */
    private void fetchPageLinks(String url, Map<String, String> pageLinks, String parentCategory) {
        Document doc = this.getDocument(url);
        Elements rows = doc.select("#zg_browseRoot ul > li > a");
        if (rows.size() == 0) {
            PageUtils.savePage(doc, "html/" + parentCategory + "-" + System.currentTimeMillis() + ".html");
            logger.error("Unable to find categories from {}", url);
            if (!UITools.confirmed("Please enter captcha manually.")) {
                if (doc.select("#captchacharacters").size() > 0) {
                    throw new RobotFoundException("Amazon captcha detected.");
                }
                return;
            }
            doc = this.getDocument(url);
            rows = doc.select("#zg_browseRoot ul > li > a");
            if (rows.size() == 0) {
                if (doc.select("#captchacharacters").size() > 0) {
                    throw new RobotFoundException("Amazon captcha detected.");
                }
                return;
            }
        }
        for (Element row : rows) {
            if (StringUtils.isNotBlank(parentCategory)) {
                pageLinks.put(row.attr("href"), parentCategory);
                continue;
            }
            String category = row.text().trim();
            if (ArrayUtils.contains(EXCLUDED_CATEGORIES, category)) {
                logger.warn("Skip category: {}", category);
                continue;
            }
            pageLinks.put(row.attr("href"), category);
        }
    }

    private void fetchSubPageLinks(Map<String, String> pageLinks) {
        Map<String, String> subPageLinks = new HashMap<>();
        for (String link : pageLinks.keySet()) {
            this.fetchPageLinks(link, subPageLinks, pageLinks.get(link));
        }
        pageLinks.putAll(subPageLinks);
    }

    private Set<Product> fetchProductsFromCategories(Map<String, String> links) {
        Set<Product> products = new HashSet<>();
        for (String link : links.keySet()) {
            String category = links.get(link);
            try {
                Set<Product> productUrls = crawlFromPage(link);
                if (CollectionUtils.isEmpty(productUrls)) {
                    productUrls = crawlFromPage(link);
                    if (CollectionUtils.isEmpty(productUrls)) {
                        logger.warn("No product found from link: {}", link);
                        return null;
                    }
                }
                productUrls.forEach(product -> product.setCategory(category));
                products.addAll(productUrls);
            } catch (Exception e) {
                logger.error("Error occurs in fetching from link {}:", link, e);
            }
        }
        return products;
    }

    /*private Set<Product> fetchProductsFromCategories(Map<String, String> links) {
        Set<Product> products = new HashSet<>();
        final CountDownLatch latch = new CountDownLatch(links.size());
        for (String link : links.keySet()) {
            String category = links.get(link);
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    try {
                        Set<Product> productUrls = crawlFromPage(link);
                        if (CollectionUtils.isEmpty(productUrls)) {
                            productUrls = crawlFromPage(link);
                            if (CollectionUtils.isEmpty(productUrls)) {
                                logger.warn("No product found from link: {}", link);
                                return null;
                            }
                        }
                        productUrls.forEach(product -> product.setCategory(category));
                        products.addAll(productUrls);
                    } catch (Exception e) {
                        logger.error("Error occurs in fetching from link {}:", link, e);
                    } finally {
                        latch.countDown();
                    }
                    return null;
                }
            }.execute();
        }

        try {
            latch.await();
        } catch (InterruptedException | RuntimeException e) {
            logger.error(Tools.getExceptionMsg(e));
        }
        return products;
    }*/

    private Set<Product> crawlFromPage(String pageLink) {
        logger.info("Crawling from page 1");
        Document doc = this.getDocument(pageLink);

        Set<Product> urls = this.fetchLinks(doc, pageLink);
        Elements elements = doc.select(".a-pagination > li.a-last > a:contains(Next page)");
        if (elements.size() == 1) {
            logger.info("Crawling from page 2");
            String nextPageUrl = elements.get(0).attr("href");
            doc = this.getDocument(nextPageUrl);
            urls.addAll(this.fetchLinks(doc, nextPageUrl));
        } else {
            logger.info("Page 2 not found");
        }

        return urls;
    }

    private Set<Product> fetchLinks(Document doc, String pageUrl) {
        Set<Product> products = new HashSet<>();
        Elements rows = doc.select("#zg-ordered-list > li.zg-item-immersion");
        if (rows.size() == 0) {
            logger.error("Unable to find the product grids");
            return products;
        }

        for (Element row : rows) {
            try {
                float stars = this.parseStars(row);
                int reviews = this.parseReviews(row);
                if (reviews > MAX_REVIEWS) {
                    logger.warn("Reviews too high: {}", reviews);
                    continue;
                }

                float price = this.parsePrice(row);
                if (price < MIN_PRICE) {
                    logger.warn("Price too low: {}", price);
                    continue;
                }

                String url = row.select("a.a-link-normal").first().attr("href");
                String title = HtmlParser.text(row, ".p13n-sc-truncate");
                String image = row.select("img").first().attr("src");

                Product product = new Product();
                product.setTitle(title);
                product.setUrl(Common.getAbsoluteUrl(url, pageUrl));

                String asin = product.parseAsin();
                if (this.asins.contains(asin)) {
                    continue;
                } else {
                    this.asins.add(asin);
                }

                product.setImage(image);
                product.setStars(stars);
                product.setReviews(reviews);
                product.setPrice(price);

                products.add(product);
            } catch (BusinessException e) {
                logger.error("Product is skipped: ", e);
            }
        }
        return products;
    }

    private float parsePrice(Element row) {
        String text = HtmlParser.text(row, ".p13n-sc-price");
        if (StringUtils.contains(text, "-")) {
            text = StringUtils.substringAfter(text, "-");
        }
        text = text.replaceAll(Regex.NON_CURRENCY.val(), StringUtils.EMPTY);
        return NumberUtils.toFloat(text.trim());
    }

    private int parseReviews(Element row) {
        Element starsElm = row.select(".a-icon-star").first();
        Elements elements = starsElm.parent().parent().select("a");
        if (elements.size() == 0) {
            throw new BusinessException("Unable to find the review element");
        }
        Element reviewElm = elements.last();
        String text = reviewElm.text().replaceAll(Regex.NON_DIGITS.val(), StringUtils.EMPTY);
        return NumberUtils.toInt(text);
    }

    private float parseStars(Element row) {
        String text = HtmlParser.text(row, ".a-icon-star");
        String[] parts = StringUtils.splitByWholeSeparator(text, "out of");
        if (parts.length != 2) {
            String message = "Unable to parse stars: " + text;
            logger.error(message);
            throw new BusinessException(message);
        }

        return NumberUtils.toFloat(parts[0].trim());
    }
}
