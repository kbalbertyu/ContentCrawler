package cn.btimes.service;

import cn.btimes.model.ccs.CCSEntity;
import cn.btimes.model.common.Config;
import cn.btimes.utils.PageUtils;
import cn.btimes.utils.Tools;
import com.alibaba.fastjson.JSON;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.PageLoadHelper;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.google.inject.Inject;
import com.mailman.model.common.WebApiResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chinese Concept Stocks crawler
 *
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/7/11 10:25
 */
public class CCSCrawler implements ServiceExecutorInterface {
    private static final String SOURCE_URL = "http://quote.meigushe.com/china/index.html";
    private static final String TEST_MODE = Tools.getCustomizingValue("TEST_MODE");
    private final Logger logger = LoggerFactory.getLogger(CCSCrawler.class);
    @Inject private ApiRequest apiRequest;
    @Inject private WebDriverLauncher webDriverLauncher;

    @Override
    public void execute(Config config) {
        WebDriver driver = webDriverLauncher.startWithoutLogin(config.getApplication().name());
        driver.get(SOURCE_URL);
        if (!PageLoadHelper.present(driver, By.cssSelector(".StockName_Text"), WaitTime.Normal)) {
            throw new BusinessException("Unable to load the page.");
        }
        Document doc = Jsoup.parse(driver.getPageSource());

        Map<String, String> links = this.parseLinks(doc);
        this.crawlLinks(driver, links, config);
    }

    private void crawlLinks(WebDriver driver, Map<String, String> links, Config config) {
        for (String label : links.keySet()) {
            String link = links.get(label);
            driver.get(link);
            if (!PageLoadHelper.present(driver, By.id("app"), WaitTime.Normal)) {
                continue;
            }
            Document doc = Jsoup.parse(driver.getPageSource());
            try {
                CCSEntity ccsEntity = this.parseEntity(doc);
                if (!ccsEntity.valid()) {
                    logger.error("Invalid entity: {}", JSON.toJSONString(ccsEntity));
                    continue;
                }
                ccsEntity.setLabel(label);
                ccsEntity.setLink(link);
                this.fixIntroWithBaidu(driver, ccsEntity);
                this.save(ccsEntity, config);
            } catch (BusinessException e) {
                logger.error("Unable to parse entity: ", e);
            }
        }
    }

    /**
     * Fetch CCS intro from Baidu Baike.
     * Replace the current one if better one found.
     */
    private void fixIntroWithBaidu(WebDriver driver, CCSEntity ccsEntity) {
        String url = String.format("https://www.baidu.com/s?ie=utf-8&wd=%s&rsv_pq=d9d39339000e6613&rqlang=cn", ccsEntity.getTitle());
        driver.get(url);
        if (!PageLoadHelper.present(driver, By.className("c-container"), WaitTime.Normal)) {
            logger.error("Baidu results page not loaded, skip current fetching: {} -> {}", ccsEntity.getLabel(), ccsEntity.getTitle());
            return;
        }

        for (int page = 1; page < 4; page++) {
            String link = driver.getCurrentUrl();
            if (!StringUtils.containsIgnoreCase(link, "baike.baidu.com")) {
                Document listDoc = Jsoup.parse(driver.getPageSource());
                link = this.parseLinkFromBaiduResultRow(listDoc);
                if (StringUtils.isBlank(link)) {
                    PageUtils.scrollToBottom(driver);
                    PageUtils.click(driver, By.cssSelector(".page-inner > a:last-child"));
                    continue;
                }
            }
            driver.get(link);
            if (!PageLoadHelper.present(driver, By.cssSelector(".lemma-summary > .para"), WaitTime.Normal)) {
                logger.error("Baidu Baike page is not loaded: {}", link);
                continue;
            }
            Document doc = Jsoup.parse(driver.getPageSource());
            String intro = this.parseBaiduBaikeIntro(doc, ccsEntity);
            if (StringUtils.length(ccsEntity.getIntro()) < StringUtils.length(intro)) {
                ccsEntity.setIntro(intro);
            }
            return;
        }
    }

    private String parseBaiduBaikeIntro(Document doc, CCSEntity ccsEntity) {
        doc.select(".sup--normal, .sup-anchor").remove();
        String body = doc.text();
        String cleanLabel = StringUtils.substringBefore(ccsEntity.getLabel(), "-");
        if (!Tools.containsAny(body, ccsEntity.getTitle(), cleanLabel)) {
            return "";
        }
        String text = StringUtils.trim(HtmlParser.texts(doc, ".lemma-summary > .para"));
        text = StringUtils.replacePattern(text, "\\[[0-9\\-]+\\]", "");
        return StringUtils.replacePattern(text, "[ ]+", " ");

    }

    private String parseLinkFromBaiduResultRow(Document doc) {
        Elements rows = doc.select(".c-container");
        for (Element row : rows) {
            Element linkElm = row.select("h3 > a").first();
            if (linkElm == null || !StringUtils.contains(linkElm.text(), "百度百科")) {
                continue;
            }
            return linkElm.attr("href");
        }
        return null;
    }

    private void save(CCSEntity ccsEntity, Config config) {
        WebApiResult result = apiRequest.post("/article/importCCSEntity?test_mode=" + TEST_MODE, JSON.toJSONString(ccsEntity), config);
        if (result == null) {
            logger.error("Unable to upload CCS entity of: {}", ccsEntity.getLabel());
        } else {
            logger.info("CCSEntity is saved: {}", ccsEntity.getLabel());
        }
    }

    private CCSEntity parseEntity(Document doc) {
        CCSEntity ccsEntity = new CCSEntity();

        this.parseTitle(ccsEntity, doc);
        this.parseSection(ccsEntity, doc);
        this.parseData(ccsEntity, doc);
        this.parseCompanyInfo(ccsEntity, doc);
        this.parseIntro(ccsEntity, doc);

        return ccsEntity;
    }

    private void parseIntro(CCSEntity ccsEntity, Document doc) {
        String intro = HtmlParser.text(doc, "#introduce > p");
        ccsEntity.setIntro(StringUtils.trim(intro));
    }

    private void parseCompanyInfo(CCSEntity ccsEntity, Document doc) {
        List<String> records = HtmlParser.texts(doc.select(".stock_company_info > p"));
        for (String record : records) {
            String title = StringUtils.substringBefore(record, "：");
            String value = StringUtils.substringAfter(record, "：");

            if (StringUtils.equals(title, "英文名称")) {
                ccsEntity.setNameEN(value);
            } else if (StringUtils.equals(title, "中文名称")) {
                ccsEntity.setNameCN(value);
            } else if (StringUtils.equals(title, "行业")) {
                ccsEntity.setIndustry(value);
            } else if (StringUtils.equals(title, "地址")) {
                ccsEntity.setAddress(value);
            } else if (StringUtils.equals(title, "电话")) {
                ccsEntity.setPhone(value);
            } else if (StringUtils.equals(title, "网站")) {
                ccsEntity.setWebsite(value);
            } else if (StringUtils.equals(title, "证券市场")) {
                ccsEntity.setStockMarket(value);
            }
        }
    }

    private void parseData(CCSEntity ccsEntity, Document doc) {
        String priceText = HtmlParser.text(doc, ".summary > .now > span");
        priceText = StringUtils.trim(StringUtils.substringAfter(priceText, "$"));
        ccsEntity.setUnitPrice(NumberUtils.toFloat(priceText));

        List<String> raising = HtmlParser.texts(doc.select(".rising > p"));
        float unitPriceRising = NumberUtils.toFloat(raising.get(0));
        ccsEntity.setUnitPriceRising(unitPriceRising);

        float unitPriceRisingPercentage = NumberUtils.toFloat(StringUtils.substringBefore(raising.get(1), "%"));
        ccsEntity.setUnitPriceRisingPercentage(unitPriceRisingPercentage);

        List<String> records = HtmlParser.texts(doc.select(".details > span"));
        for (String record : records) {
            if (Tools.containsAny(record, "NaN", "{{")) {
                throw new BusinessException("Invalid data found on page: " + record);
            }
            String title = StringUtils.substringBefore(record, "：");
            String value = StringUtils.substringAfter(record, "：");
            if (StringUtils.equals(title, "昨收")) {
                ccsEntity.setYesterdayClosingPrice(NumberUtils.toFloat(value));
            } else if (StringUtils.equals(title, "52周最高")) {
                ccsEntity.setWeek52TopUnitPrice(NumberUtils.toFloat(value));
            } else if (StringUtils.equals(title, "52周最低")) {
                ccsEntity.setWeek52BottomUnitPrice(NumberUtils.toFloat(value));
            } else if (StringUtils.equals(title, "总市值")) {
                ccsEntity.setTotalMarketValueText(value);
                String cleanValue = value.replaceAll("[^0-9,.-]", "");
                ccsEntity.setTotalMarketValue(NumberUtils.toFloat(cleanValue));
            } else if (StringUtils.equals(title, "总股本")) {
                ccsEntity.setTotalStocks(NumberUtils.toLong(value));
            } else if (StringUtils.equals(title, "市盈率")) {
                ccsEntity.setPriceEarningRatio(NumberUtils.toFloat(value));
            }
        }
    }

    private void parseSection(CCSEntity ccsEntity, Document doc) {
        String section = HtmlParser.text(doc, ".now_time");
        ccsEntity.setSection(this.extractValue(section));
    }

    private String extractValue(String text) {
        return StringUtils.trim(StringUtils.substringAfter(text, "："));
    }

    private void parseTitle(CCSEntity ccsEntity, Document doc) {
        String fullTitle = HtmlParser.text(doc, "#app > .name > h2");
        String title = StringUtils.substringBefore(fullTitle, "(");
        ccsEntity.setTitle(title);

        String codes = StringUtils.substringBetween(fullTitle, "(", ")");
        ccsEntity.setStockClass(StringUtils.substringBefore(codes, ":"));
        ccsEntity.setStockCode(StringUtils.substringAfter(codes, ":"));
    }

    private Map<String, String> parseLinks(Document doc) {
        Elements linkElms = doc.select(".StockName_Text a");
        Map<String, String> links = new HashMap<>();
        for (Element linkElm : linkElms) {
            links.put(linkElm.text(), linkElm.attr("href"));
        }
        return links;
    }
}
