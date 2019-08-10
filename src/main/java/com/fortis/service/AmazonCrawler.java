package com.fortis.service;

import cn.btimes.model.common.Config;
import cn.btimes.service.ServiceExecutorInterface;
import cn.btimes.utils.Common;
import cn.btimes.utils.PageUtils;
import com.amzass.enums.common.Country;
import com.amzass.enums.common.Directory;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.amzass.utils.common.RegexUtils.Regex;
import com.amzass.utils.common.Tools;
import com.fortis.model.AmzHot;
import com.fortis.model.Product;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/8/8 9:06
 */
public class AmazonCrawler implements ServiceExecutorInterface {
    private static final int MAX_REVIEWS = 100;
    private static final float MIN_PRICE = 15f;
    private static final String LINE_SEPARATOR = "----------------";
    private final Logger logger = LoggerFactory.getLogger(AmazonCrawler.class);
    private static final String[] EXCLUDED_CATEGORIES = {
        "Amazon Devices & Accessories",
        "Amazon Launchpad",
        "Appliances",
        "Apps & Games",
        "Arts, Crafts & Sewing",
        "Audible Books & Originals",
        "Books",
        "CDs & Vinyl",
        "Collectible Currencies",
        "Digital Music",
        "Gift Cards",
        "Grocery & Gourmet Food",
        "Handmade Products",
        "Industrial & Scientific",
        "Kindle Store",
        "Magazine Subscriptions",
        "Movies & TV",
        "Musical Instruments",
        "Software",
        "Video Games"
    };

    @Override
    public void execute(Config config) {
        this.crawlFromHotLists();
    }

    /**
     * Crawl from: Best Sellers, New Releases, Movers & Shakers, Most Wished For
     * Requires: price(>= MIN_PRICE), stars(< MAX_STARS), reviews(< MIN_PRICE)
     */
    private void crawlFromHotLists() {
        Map<AmzHot, Set<Product>> products = new HashMap<>();
        try {
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
                    products.put(amzHot, list);
                } catch (BusinessException e) {
                    logger.error("Unknown exception found in fetching from {}: ", amzHot.name(), e);
                }
            }
        } finally {
            this.saveLinks(products);
        }
    }

    private void saveLinks(Map<AmzHot, Set<Product>> products) {
        if (products.size() == 0) {
            logger.warn("No products found.");
            return;
        }
        String date = DateFormatUtils.format(new Date(), "yyyy-MM-dd-HH-mm");
        File file = FileUtils.getFile(Directory.Tmp.path(), "amzHot-" + date + ".md");

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
        Document doc = PageUtils.getDocumentByJsoup(url);

        // Fetch page links of categories
        Elements rows = doc.select("#zg_browseRoot > ul > li > a");
        if (rows.size() == 0) {
            logger.error("Unable to find categories from {} page", amzHot.name());
            return null;
        }
        Map<String, String> pageLinks = new HashMap<>();
        for (Element row : rows) {
            String category = row.text().trim();
            if (ArrayUtils.contains(EXCLUDED_CATEGORIES, category)) {
                logger.warn("Skip category: {}", category);
                continue;
            }
            pageLinks.put(row.attr("href"), category);
        }

        if (pageLinks.size() == 0) {
            logger.error("Unable to find category page links");
            return null;
        }
        // Fetch product links from category pages
        return this.fetchProductsFromCategories(pageLinks);
    }

    private Set<Product> fetchProductsFromCategories(Map<String, String> links) {
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
                            logger.warn("No product found from link: {}", link);
                            return null;
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
    }

    private Set<Product> crawlFromPage(String pageLink) {
        logger.info("Crawling from page 1");
        Document doc = PageUtils.getDocumentByJsoup(pageLink);

        Set<Product> urls = this.fetchLinks(doc, pageLink);
        Elements elements = doc.select(".a-pagination > li.a-last > a:contains(Next page)");
        if (elements.size() == 1) {
            logger.info("Crawling from page 2");
            String nextPageUrl = elements.get(0).attr("href");
            doc = PageUtils.getDocumentByJsoup(nextPageUrl);
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
