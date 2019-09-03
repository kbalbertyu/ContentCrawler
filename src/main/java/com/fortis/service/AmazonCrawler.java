package com.fortis.service;

import cn.btimes.model.common.Config;
import cn.btimes.service.ServiceExecutorInterface;
import cn.btimes.service.WebDriverLauncher;
import cn.btimes.utils.Common;
import cn.btimes.utils.PageUtils;
import com.amzass.database.DBUtils;
import com.amzass.enums.common.Country;
import com.amzass.enums.common.Directory;
import com.amzass.service.common.ApplicationContext;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.ui.utils.UITools;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Constants;
import com.amzass.utils.common.DateHelper;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.amzass.utils.common.Exceptions.RobotFoundException;
import com.amzass.utils.common.RegexUtils.Regex;
import com.amzass.utils.common.Tools;
import com.fortis.model.AmzHot;
import com.fortis.model.Link;
import com.fortis.model.Product;
import com.google.inject.Inject;
import com.kber.commons.DBManager;
import com.mailman.enums.common.ServiceEnums.DaysInterval;
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
import org.nutz.dao.Cnd;
import org.nutz.dao.Dao;
import org.nutz.dao.impl.FileSqlManager;
import org.nutz.dao.impl.NutDao;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/8/8 9:06
 */
public class AmazonCrawler implements ServiceExecutorInterface {
    private static final int MAX_REVIEWS = 50;
    private static final float MIN_PRICE = 10f;
    private static final float MAX_PRICE = 50f;
    private static final String LINE_SEPARATOR = "----------------";
    private static final String PRODUCT_TABLE = "product";
    private static final boolean AMAZON_GENERATE_FILES_ONLY = StringUtils.isNotBlank(Tools.getCustomizingValue("AMAZON_GENERATE_FILES_ONLY"));
    private String date = DateFormatUtils.format(new Date(), "yyyy-MM-dd");
    private final Logger logger = LoggerFactory.getLogger(AmazonCrawler.class);
    private boolean skipBlankTitle = false;
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
        "Collectible Currencies",
        "Computers & Accessories",
        "Digital Music",
        "Electronics",
        "Entertainment Collectibles",
        "Grocery & Gourmet Food",
        "Handmade Products",
        "Kindle Store",
        "Movies & TV",
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
    @Inject private DBManager dbManager;
    private WebDriver driver;
    private Set<String> asins = new HashSet<>();

    @Override
    public void execute(Config config) {
        this.initDB();
        if (AMAZON_GENERATE_FILES_ONLY) {
            logger.info("Generate amazon files only.");
            this.generateFiles();
            return;
        }
        this.crawlFromHotLists();
    }

    private void initDB() {
        Map<String, String> map = Tools.loadPropCfg(com.amzass.enums.common.Config.JDBC);
        String url = map.get("url");
        try {
            Dao dao = new NutDao(DBUtils.buildSqliteDataSource(url), new FileSqlManager(com.amzass.enums.common.Config.DBTables.fileName()));
            if (!dao.exists(PRODUCT_TABLE)) {
                dao.execute(dao.sqls().create(PRODUCT_TABLE));
            }
        } catch (ClassNotFoundException e) {
            logger.error("Unable to create table of {}", PRODUCT_TABLE, e);
        }
    }

    /**
     * Crawl from: Best Sellers, New Releases, Movers & Shakers, Most Wished For
     * Requires: price(>= MIN_PRICE), stars(< MAX_STARS), reviews(< MIN_PRICE)
     */
    private void crawlFromHotLists() {
        driver = webDriverLauncher.startWithoutLogin(this.getClass().getSimpleName());
        Country country = Country.US;
        for (AmzHot amzHot : AmzHot.values()) {
            logger.info("Crawling from: {}", amzHot.name());
            try {
                Set<Product> list = this.crawlFromHotList(country, amzHot);
                if (CollectionUtils.isEmpty(list)) {
                    logger.warn("No product found from {}", amzHot.name());
                    continue;
                }
                logger.info("Found {} products from {}", list.size(), amzHot.name());
            } catch (BusinessException e) {
                logger.error("Unknown exception found in fetching from {}: ", amzHot.name(), e);
            } catch (RobotFoundException e) {
                logger.error("Skip crawling due to captcha detected: ", e);
                break;
            }
        }
        this.generateFiles();
    }

    private void generateFiles() {
        String recordDate = date;
        Cnd cnd = (Cnd) Cnd.NEW()
            .groupBy("category")
            .having(Cnd.where("modifiedDate", Constants.EQUALS, recordDate));
        List<Product> products = dbManager.query(Product.class, cnd);
        if (products.size() == 0) {
            logger.warn("No product crawled today.");
            Date lastDate = DateHelper.daysBefore(DaysInterval.Day);
            recordDate = DateFormatUtils.format(lastDate, "yyyy-MM-dd");
            cnd = (Cnd) Cnd.NEW()
                .groupBy("category")
                .having(Cnd.where("modifiedDate", Constants.EQUALS, recordDate));
            products = dbManager.query(Product.class, cnd);
            if (products.size() == 0) {
                logger.warn("No product crawled yesterday.");
            } else {
                logger.info("Found {} products yesterday.", products.size());
            }
        } else {
            logger.info("Found {} products today.", products.size());
        }

        for (Product product : products) {
            this.generateFilesByCategory(product.getCategory(), recordDate);
        }
    }

    private void generateFilesByCategory(String category, String date) {
        for (AmzHot hot : AmzHot.values()) {
            Cnd cnd = (Cnd) Cnd.NEW()
                .and("category", Constants.EQUALS, category)
                .and("hot", Constants.EQUALS, hot.name())
                .and("modifiedDate", Constants.EQUALS, date)
                .desc("times");
            List<Product> products = dbManager.query(Product.class, cnd);
            if (products.size() == 0) {
                continue;
            }
            this.saveMDFile(products, hot, category);
        }
    }

    private void saveMDFile(List<Product> products, AmzHot amzHot, String category) {
        if (products.size() == 0) {
            logger.warn("No products found.");
            return;
        }
        File file = FileUtils.getFile(Directory.Tmp.path(), category, amzHot.name() + ".md");

        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(amzHot.name())
            .append(StringUtils.LF)
            .append(category)
            .append(StringUtils.LF)
            .append(LINE_SEPARATOR)
            .append(StringUtils.LF);

        for (Product product : products) {
            if (StringUtils.isBlank(product.getTitle())) {
                continue;
            }
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

        Tools.writeStringToFile(file, sb.toString());
        logger.info("Urls saved to file: {}", file.getAbsolutePath());
    }

    private Set<Product> crawlFromHotList(Country country, AmzHot amzHot) {
        String url = country.securedBaseUrl() + amzHot.path;
        Link hotPage = new Link(url);

        Set<Link> pageLinks = new HashSet<>();
        this.fetchPageLinks(hotPage, pageLinks);

        if (pageLinks.size() == 0) {
            logger.error("Unable to find category page links");
            return null;
        }
        this.fetchSubPageLinks(pageLinks);
        // Fetch product links from category pages
        return this.fetchProductsFromCategories(pageLinks, amzHot);
    }

    private Document getDocument(String url) {
        try {
            driver.get(url);
            WaitTime.Normal.execute();
            return Jsoup.parse(driver.getPageSource());
        } catch (TimeoutException e) {
            driver.close();
            driver.quit();

            driver = webDriverLauncher.startWithoutLogin(this.getClass().getSimpleName());
            driver.get(url);
            WaitTime.Normal.execute();
            return Jsoup.parse(driver.getPageSource());
        }
    }

    /**
     * Fetch page links of categories
     */
    private void fetchPageLinks(Link link, Set<Link> pageLinks) {
        Document doc = this.getDocument(link.getUrl());
        Elements rows = doc.select("#zg_browseRoot ul > li > a");
        if (rows.size() == 0) {
            PageUtils.savePage(doc, "html/" + link.getCategory() + "-" + System.currentTimeMillis() + ".html");
            logger.error("Unable to find categories from {}", link);
            if (!UITools.confirmed("Please enter captcha manually.")) {
                if (doc.select("#captchacharacters").size() > 0) {
                    throw new RobotFoundException("Amazon captcha detected.");
                }
                return;
            }
            doc = this.getDocument(link.getUrl());
            rows = doc.select("#zg_browseRoot ul > li > a");
            if (rows.size() == 0) {
                if (doc.select("#captchacharacters").size() > 0) {
                    throw new RobotFoundException("Amazon captcha detected.");
                }
                return;
            }
        }
        for (Element row : rows) {
            String category = row.text().trim();
            if (ArrayUtils.contains(EXCLUDED_CATEGORIES, category)) {
                logger.warn("Skip category: {}", category);
                continue;
            }
            String parent;
            if (StringUtils.isBlank(link.getCategory())) {
                parent = category;
                category = "";
            } else {
                parent = link.getCategory();
            }
            Link pageLink = new Link(row.attr("href"), parent, category);
            pageLinks.add(pageLink);
        }
    }

    private void fetchSubPageLinks(Set<Link> pageLinks) {
        Set<Link> subPageLinks = new HashSet<>();
        for (Link link : pageLinks) {
            this.fetchPageLinks(link, subPageLinks);
        }
        pageLinks.addAll(subPageLinks);
    }

    private Set<Product> fetchProductsFromCategories(Set<Link> links, AmzHot amzHot) {
        Set<Product> products = new HashSet<>();
        for (Link link : links) {
            try {
                Set<Product> productSet = crawlFromPage(link.getUrl());
                if (CollectionUtils.isEmpty(productSet)) {
                    productSet = crawlFromPage(link.getUrl());
                    if (CollectionUtils.isEmpty(productSet)) {
                        logger.warn("No product found from link: {}", link);
                        continue;
                    }
                }
                productSet.forEach(product -> {
                    product.setCategory(link.getCategory());
                    product.setSubCategory(link.getSubCategory());
                    product.setHot(amzHot.name());
                    product.parseAsin();
                    product.setModifiedDate(date);

                    Product find = dbManager.readById(product.getAsin(), Product.class);
                    if (find != null) {
                        product.setTimes(find.getTimes() + 1);
                    } else {
                        product.setStartDate(date);
                    }
                    dbManager.save(product, Product.class);
                });
                products.addAll(productSet);
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

        Set<Product> products = this.fetchLinks(doc, pageLink);
        Elements elements = doc.select(".a-pagination > li.a-last > a:contains(Next page)");
        if (elements.size() == 1) {
            logger.info("Crawling from page 2");
            String nextPageUrl = elements.get(0).attr("href");
            doc = this.getDocument(nextPageUrl);
            products.addAll(this.fetchLinks(doc, nextPageUrl));
        } else {
            logger.info("Page 2 not found");
        }

        return products;
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
                if (price < MIN_PRICE || price > MAX_PRICE) {
                    logger.warn("Price not between {} and {}: {}", MIN_PRICE, MAX_PRICE, price);
                    continue;
                }

                String url = row.select("a.a-link-normal").first().attr("href");
                String title = HtmlParser.text(row, ".p13n-sc-truncate", ".p13n-sc-truncated");
                if (StringUtils.isBlank(title)) {
                    if (!skipBlankTitle && UITools.confirmed("Title is blank, skip?")) {
                        skipBlankTitle = true;
                    }
                    if (skipBlankTitle) {
                        logger.warn("Title is blank, skip this product: {}", url);
                        continue;
                    }
                    logger.warn("Title is blank: {}", url);
                }
                String image = row.select("img").first().attr("src");

                Product product = new Product();
                product.setTitle(title);
                product.setUrl(Common.getAbsoluteUrl(url, pageUrl));

                String asin = product.parseAsin();
                product.setAsin(asin);
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

    public static void main(String[] args) {
        ApplicationContext.getBean(AmazonCrawler.class).generateFiles();
        System.exit(0);
    }
}
