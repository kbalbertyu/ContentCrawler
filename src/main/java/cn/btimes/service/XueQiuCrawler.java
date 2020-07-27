package cn.btimes.service;

import cn.btimes.model.ccs.CCSData;
import cn.btimes.model.ccs.CCSFinanceData;
import cn.btimes.model.ccs.CCSFinanceData.FinanceData;
import cn.btimes.model.ccs.CCSInfo;
import cn.btimes.model.common.Config;
import cn.btimes.utils.Common;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.PageLoadHelper;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Constants;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.amzass.utils.common.HttpUtils;
import com.amzass.utils.common.PageUtils;
import com.amzass.utils.common.RegexUtils.Regex;
import com.amzass.utils.common.Tools;
import com.google.inject.Inject;
import com.mailman.model.common.WebApiResult;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * 中概股：雪球数据抓取
 *
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/7/23 17:15
 */
public class XueQiuCrawler implements ServiceExecutorInterface {
    private static final String CHART_SOURCE_URL = "https://q.stock.sohu.com/us/%s.html";
    private static final String DOWNLOAD_PATH = "downloads";
    private static final String CHARTS_UPLOAD_URL = "/article/uploadCCSCharts";
    private final Logger logger = LoggerFactory.getLogger(XueQiuCrawler.class);
    @Inject private ApiRequest apiRequest;
    @Inject private WebDriverLauncher webDriverLauncher;
    private static final String SOURCE_URL = "https://xueqiu.com/hq#exchange=US&industry=3_3&firstName=3";

    @Override
    public void execute(Config config) {
        String profile = StringUtils.join(config.getArgs(), "-");
        WebDriver driver = webDriverLauncher.startWithoutLogin(profile);
        driver.get(SOURCE_URL);
        if (!PageLoadHelper.present(driver, By.id("stockList"), WaitTime.Normal)) {
            throw new BusinessException("Unable to load the page.");
        }

        if (ArrayUtils.contains(config.getArgs(), "CCSData")) {
            Set<CCSData> ccsDataSet = this.crawlList(driver);
            for (CCSData ccsData : ccsDataSet) {
                Document doc = this.loadEntityPage(driver, ccsData.getStockCode());
                if (doc == null) {
                    continue;
                }
                this.crawlCCSData(doc, ccsData);
                this.uploadCCSData(ccsData, config);
            }
        }

        if (ArrayUtils.contains(config.getArgs(), "CCSDataUpdate")) {
            List<String> stockCodes = this.fetchStockCodes(config);
            for (String stockCode : stockCodes) {
                Document doc = this.loadEntityPage(driver, stockCode);
                if (doc == null) {
                    continue;
                }
                CCSData ccsData = new CCSData();
                ccsData.setStockCode(stockCode);

                this.crawlCCSData(doc, ccsData);
                this.uploadCCSData(ccsData, config);
            }
        }

        if (ArrayUtils.contains(config.getArgs(), "CCSInfo")) {
            List<String> stockCodes = this.fetchStockCodes(config);
            for (String stockCode : stockCodes) {
                Document doc = this.loadEntityPage(driver, stockCode);
                if (doc == null) {
                    continue;
                }
                CCSInfo ccsInfo = this.crawlCCSInfo(driver, doc, stockCode);
                this.uploadCCSInfo(ccsInfo, config);
            }
        }

        if (ArrayUtils.contains(config.getArgs(), "CCSChart")) {
            List<String> stockCodes = this.fetchStockCodes(config);
            if (CollectionUtils.isNotEmpty(stockCodes)) {
                for (String stockCode : stockCodes) {
                    this.crawlCharts(stockCode, config, driver);
                }
            }
        }
    }

    private void crawlCharts(String stockCode, Config config, WebDriver driver) {
        String url = String.format(CHART_SOURCE_URL, stockCode);
        driver.get(url);
        if (!PageLoadHelper.present(driver, By.id("mlineImg"), WaitTime.Normal)) {
            return;
        }

        PageUtils.click(driver, By.id("yline"));
        WaitTime.Shortest.execute();
        PageUtils.click(driver, By.id("kline"));
        WaitTime.Shortest.execute();

        Document doc = Jsoup.parse(driver.getPageSource());
        Elements imageElms = doc.select("#hqimg > img");
        Set<File> files = new HashSet<>();
        for (Element imageElm : imageElms) {
            String id = imageElm.id();
            String src = "https:" + imageElm.attr("src");

            File file = this.makeDownloadFile(stockCode, id);
            try {
                this.download(src, file);
                files.add(file);
            } catch (BusinessException e) {
                logger.error("Unable to download the file: {}", src);
            }
        }

        this.uploadCharts(files, config);
    }

    private void uploadCharts(Set<File> files, Config config) {
        Connection conn = Jsoup.connect(ApiRequest.getFullUrl(CHARTS_UPLOAD_URL, config))
            .validateTLSCertificates(false)
            .userAgent("Mozilla")
            .method(Method.POST)
            .timeout(0).maxBodySize(0);
        int i = 0;
        for (File file : files) {
            try {
                FileInputStream fs = new FileInputStream(file);
                conn.data("files[" + i + "]", file.getAbsolutePath(), fs);
                i++;
            } catch (IOException e) {
                String message = String.format("Unable to download file: %s", file.getName());
                logger.error(message, e);
            }
        }

        try {
            String body = conn.execute().body();
            logger.info("File uploaded: {}", body);
        } catch (IOException e) {
            logger.error("Unable to upload ccs chart files: ", e);
        }
    }

    private void download(String url, File file) {
        HttpGet get = HttpUtils.prepareHttpGet(cn.btimes.utils.Tools.encodeUrl(url));
        CloseableHttpClient httpClient = HttpClients.createDefault();
        BasicHttpContext localContext = new BasicHttpContext();

        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            CloseableHttpResponse resp = null;
            InputStream is = null;
            try {
                resp = httpClient.execute(get, localContext);
                int status = resp.getStatusLine().getStatusCode();
                if (status == HttpStatus.SC_OK) {
                    is = resp.getEntity().getContent();
                    FileUtils.copyInputStreamToFile(is, file);
                    return;
                }
                String message = String.format("Failed to execute file download request: fileName=%s, url=%s, status=%s.", file.getName(), url, status);
                logger.error(message);
            } catch (Exception ex) {
                String message = String.format("Failed to download file of %s： %s", url, Tools.getExceptionMsg(ex));
                logger.error(message);
            } finally {
                get.releaseConnection();
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(resp);
            }
        }
        throw new BusinessException(String.format("Failed to execute %s file download request after retried.", url));
    }

    private File makeDownloadFile(String stockCode, String id) {
        File file = FileUtils.getFile(DOWNLOAD_PATH, "ccs", stockCode + "-" + id + ".png");
        if (file.exists()) {
            FileUtils.deleteQuietly(file);
            file = FileUtils.getFile(DOWNLOAD_PATH, "ccs", stockCode + "-" + id + ".png");
        }
        if (!file.canWrite()) {
            file.setWritable(true);
        }
        return file;
    }

    private List<String> fetchStockCodes(Config config) {
        WebApiResult result = apiRequest.get("/article/fetchCCSCodes", config);
        if (result == null) {
            return null;
        }
        return JSONObject.parseArray(result.getData(), String.class);
    }

    private void uploadCCSInfo(CCSInfo ccsInfo, Config config) {
        String dataText = JSON.toJSONString(ccsInfo);
        WebApiResult result = apiRequest.post("/article/importCCSInfo", dataText, config);
        if (result == null) {
            logger.error("CCS Info upload fails: {}", dataText);
        } else {
            logger.info("CCS Info uploaded: {}", ccsInfo.getStockCode());
        }
    }

    private void uploadCCSData(CCSData ccsData, Config config) {
        String dataText = JSON.toJSONString(ccsData);
        WebApiResult result = apiRequest.post("/article/importCCSData", dataText, config);
        if (result == null) {
            logger.error("CCS Data upload fails: {}", dataText);
        } else {
            logger.info("CCS Data uploaded: {} -> {}", ccsData.getStockCode(), ccsData.getStockName());
        }
    }

    private Document loadEntityPage(WebDriver driver, String stockCode) {
        String url = Common.getAbsoluteUrl("/S/" + stockCode, SOURCE_URL);
        driver.get(url);
        if (!PageLoadHelper.present(driver, By.className("stock-name"), WaitTime.Normal)) {
            logger.error("Unable to load the page of: " + stockCode);
            return null;
        }

        return Jsoup.parse(driver.getPageSource());
    }

    private void crawlCCSData(Document doc, CCSData ccsData) {
        this.parseStockName(doc, ccsData);

        String current = HtmlParser.text(doc, ".stock-current");
        ccsData.setCurrentPrice(this.parseFloat(current));

        String change = HtmlParser.text(doc, ".stock-change");
        String[] changeParts = StringUtils.split(change, " ");
        ccsData.setUpDown(this.parseFloat(changeParts[0]));
        ccsData.setUpDownPercent(this.parseFloat(changeParts[1]));

        List<String> issues = HtmlParser.texts(doc.select(".stock-issue"));
        ccsData.setStockIssues(issues);

        List<String> stockTimeInfo = HtmlParser.texts(doc.select(".stock-time > span"));
        if (stockTimeInfo.size() == 2) {
            ccsData.setStockStatus(stockTimeInfo.get(0));
            ccsData.setStockTime(stockTimeInfo.get(1));
        }

        Elements beforeAfterElms = doc.select(".before-after-quote > span");
        List<String> beforeAfterData = HtmlParser.texts(beforeAfterElms);
        beforeAfterElms.remove();
        String beforeAfter = StringUtils.trim(HtmlParser.text(doc, ".before-after"));
        if (StringUtils.isNotBlank(beforeAfter) && beforeAfterData.size() == 3) {
            ccsData.setBeforeAfter(beforeAfter);
            ccsData.setBeforeAfterPrice(this.parseFloat(beforeAfterData.get(0)));
            ccsData.setBeforeAfterDiff(this.parseFloat(beforeAfterData.get(1)));
            ccsData.setBeforeAfterPercent(this.parseFloat(beforeAfterData.get(2)));
        }

        Elements tds = doc.select(".quote-info td");
        for (Element td : tds) {
            String text = td.text();
            this.parseDataPair(text, ccsData);
        }
    }

    /**
     * 最高：82.46	今开：81.10	成交量：602.21万股	换手：0.50%
     * 最低：78.40	昨收：80.76	成交额：4.84亿	振幅：5.03%
     * 52周最高：98.96	量比：0.93	市盈率(TTM)：亏损	市净率：30.60
     * 52周最低：20.73	委比：-28.57%	市盈率(静)：亏损	市销率：20.86
     * 每股收益：-0.16	股息(TTM)：--	每手股数：1	总市值：946.72亿
     * 每股净资产：2.66	股息率(TTM)：--	最小价差：0.01	总股本：11.98亿
     * 机构持股：--	Beta：--	空头回补天数：4.34	货币单位：USD
     */
    private void parseDataPair(String text, CCSData ccsData) {
        String label = StringUtils.substringBefore(text, "：").trim();
        String value = StringUtils.substringAfter(text, "：").trim();

        if (StringUtils.equals(label, "最高")) {
            ccsData.setTopUnitPrice(this.parseFloat(value));
            return;
        }

        if (StringUtils.equals(label, "最低")) {
            ccsData.setBottomUnitPrice(this.parseFloat(value));
            return;
        }

        if (StringUtils.equals(label, "今开")) {
            ccsData.setTodayOpenUnitPrice(this.parseFloat(value));
            return;
        }

        if (StringUtils.equals(label, "昨收")) {
            ccsData.setYesterdayCloseUnitPrice(this.parseFloat(value));
            return;
        }

        if (StringUtils.equals(label, "成交量")) {
            ccsData.setDealVolume(this.parseFloat(value));
            ccsData.setDealVolumeUnit(this.parseUnit(value));
            return;
        }

        if (StringUtils.equals(label, "成交额")) {
            ccsData.setTurnover(this.parseFloat(value));
            ccsData.setTurnoverUnit(this.parseUnit(value));
            return;
        }

        if (StringUtils.equals(label, "换手")) {
            ccsData.setTurnoverRatioPercent(this.parseFloat(value));
            return;
        }

        if (StringUtils.equals(label, "振幅")) {
            ccsData.setAmplitudePercent(this.parseFloat(value));
            return;
        }

        if (StringUtils.equals(label, "52周最高")) {
            ccsData.setWeek52TopUnitPrice(this.parseFloat(value));
            return;
        }

        if (StringUtils.equals(label, "52周最低")) {
            ccsData.setWeek52BottomUnitPrice(this.parseFloat(value));
            return;
        }

        if (StringUtils.equals(label, "量比")) {
            ccsData.setQuantityRatio(this.parseFloat(value));
            return;
        }

        if (StringUtils.equals(label, "委比")) {
            ccsData.setWeibiPercent(this.parseFloat(value));
            return;
        }

        if (StringUtils.equals(label, "市盈率(TTM)")) {
            ccsData.setPeRatioText(value);
            return;
        }

        if (StringUtils.equals(label, "市盈率(静)")) {
            ccsData.setPeRatioStaticText(value);
            return;
        }

        if (StringUtils.equals(label, "市净率")) {
            ccsData.setPbRatio(this.parseFloat(value));
            return;
        }

        if (StringUtils.equals(label, "市销率")) {
            ccsData.setMarketSalesRate(this.parseFloat(value));
            return;
        }

        if (StringUtils.equals(label, "每股收益")) {
            ccsData.setEarningsPerShare(this.parseFloat(value));
            return;
        }

        if (StringUtils.equals(label, "每股净资产")) {
            ccsData.setNetAssetsPerShare(this.parseFloat(value));
            return;
        }

        if (StringUtils.equals(label, "股息(TTM)")) {
            ccsData.setDividends(value);
            return;
        }

        if (StringUtils.equals(label, "股息率(TTM)")) {
            ccsData.setDividendYield(value);
            return;
        }

        if (StringUtils.equals(label, "每手股数")) {
            ccsData.setSharesPerLot(NumberUtils.toInt(value));
            return;
        }

        if (StringUtils.equals(label, "最小价差")) {
            ccsData.setMinimumSpread(this.parseFloat(value));
            return;
        }

        if (StringUtils.equals(label, "总市值")) {
            ccsData.setMarketValue(this.parseFloat(value));
            ccsData.setMarketValueUnit(this.parseUnit(value));
            return;
        }

        if (StringUtils.equals(label, "总股本")) {
            ccsData.setTotalEquity(this.parseFloat(value));
            ccsData.setTotalEquityUnit(this.parseUnit(value));
            return;
        }

        if (StringUtils.equals(label, "机构持股")) {
            ccsData.setInstitutionalHoldings(value);
            return;
        }

        if (StringUtils.equals(label, "Beta")) {
            ccsData.setBeta(this.parseFloat(value));
            return;
        }

        if (StringUtils.equals(label, "空头回补天数")) {
            ccsData.setShortCoveringDays(this.parseFloat(value));
            return;
        }

        if (StringUtils.equals(label, "货币单位")) {
            ccsData.setCurrency(value);
        }
    }

    private void parseStockName(Document doc, CCSData ccsData) {
        String name = HtmlParser.text(doc, ".stock-name");
        String stockName = StringUtils.substringBefore(name, "(");
        ccsData.setStockName(stockName);

        String codes = StringUtils.substringBetween(name, "(", ")");
        ccsData.setQuotation(StringUtils.substringBefore(codes, ":"));
    }

    private CCSInfo crawlCCSInfo(WebDriver driver, Document doc, String stockCode) {
        CCSInfo ccsInfo = new CCSInfo();
        ccsInfo.setStockCode(stockCode);
        doc.select(".fold, .profile-summary").remove();

        String info = HtmlParser.text(doc, ".profile-detail");

        String introShort = StringUtils.substringBefore(info, "公司网站：").trim();
        ccsInfo.setIntroShort(introShort);

        String website = StringUtils.substringBetween(info, "公司网站：", "公司地址：");
        ccsInfo.setWebsite(StringUtils.trim(website));

        String address = StringUtils.substringBetween(info, "公司地址：", "公司电话：");
        ccsInfo.setAddress(StringUtils.trim(address));

        String phone = StringUtils.substringAfter(info, "公司电话：").trim();
        ccsInfo.setPhone(phone);

        HashMap<FinanceData, CCSFinanceData> financeDataMap = new HashMap<>();

        Elements linkElms = doc.select(".stock-links a");
        for (Element linkElm : linkElms) {
            String url = linkElm.attr("href");
            String text = linkElm.text().trim();

            if (StringUtils.equals(text, "公司简介")) {
                driver.get(Common.getAbsoluteUrl(url, SOURCE_URL));
                if (!PageLoadHelper.present(driver, By.className("gsjj"), WaitTime.Normal)) {
                    logger.error("Page not found: {}", text);
                    continue;
                }
                Document pageDoc = Jsoup.parse(driver.getPageSource());
                String intro = pageDoc.select(".gsjj").outerHtml().trim();
                ccsInfo.setIntro(intro);
                continue;
            }

            if (StringUtils.equals(text, "公司高管")) {
                driver.get(Common.getAbsoluteUrl(url, SOURCE_URL));
                if (!PageLoadHelper.present(driver, By.className("brief-info"), WaitTime.Normal)) {
                    logger.error("Page not found: {}", text);
                    continue;
                }
                Document pageDoc = Jsoup.parse(driver.getPageSource());
                String intro = pageDoc.select(".brief-info").outerHtml().trim();
                ccsInfo.setExecutives(intro);
                continue;
            }

            if (StringUtils.equals(text, "内部持股")) {
                ccsInfo.setInternalShareholdingUrl(url);
                continue;
            }

            if (StringUtils.equals(text, "所属指数")) {
                ccsInfo.setAffiliationIndexUrl(url);
                continue;
            }

            if (StringUtils.equals(text, "盘前交易")) {
                ccsInfo.setPreMarketTradeUrl(url);
                continue;
            }

            if (StringUtils.equals(text, "盘后交易")) {
                ccsInfo.setAfterHoursTradingUrl(url);
                continue;
            }

            if (StringUtils.equals(text, "历史价格")) {
                ccsInfo.setHistoricalPriceUrl(url);
                continue;
            }

            if (StringUtils.equals(text, "期权交易")) {
                ccsInfo.setOptionTradingUrl(url);
                continue;
            }

            if (StringUtils.equals(text, "空仓数据")) {
                ccsInfo.setShortPositionDataUrl(url);
                continue;
            }

            if (StringUtils.equals(text, "内部交易")) {
                ccsInfo.setInternalTransactionUrl(url);
                continue;
            }

            if (StringUtils.equals(text, "市场预期")) {
                ccsInfo.setMarketExpectationsUrl(url);
                continue;
            }

            if (StringUtils.equals(text, "SEC文件")) {
                ccsInfo.setSecFileUrl(url);
                continue;
            }

            if (StringUtils.equals(text, "财报公告(中)")) {
                ccsInfo.setFinanceReportCNUrl(url);
                continue;
            }

            if (StringUtils.equals(text, "财报公告(英)")) {
                ccsInfo.setFinanceReportENUrl(url);
                continue;
            }

            if (StringUtils.equals(text, "电话会议实录")) {
                ccsInfo.setPhoneConferenceRecordUrl(url);
                continue;
            }

            if (StringUtils.equals(text, "收益预估")) {
                ccsInfo.setRevenueForecastUrl(url);
                continue;
            }

            if (StringUtils.equals(text, "评级变化")) {
                ccsInfo.setRatingChangesUrl(url);
                continue;
            }

            if (StringUtils.equals(text, "研究报告")) {
                ccsInfo.setResearchReportUrl(url);
                continue;
            }

            FinanceData data = FinanceData.parse(text);
            if (data == null) continue;

            driver.get(Common.getAbsoluteUrl(url, SOURCE_URL));
            if (!PageLoadHelper.present(driver, By.className("stock-info-content"), WaitTime.Normal)) {
                continue;
            }

            CCSFinanceData financeData = this.crawlSingleFinanceData(driver);
            if (financeData == null) continue;
            financeDataMap.put(data, financeData);
        }

        ccsInfo.setFinanceData(financeDataMap);
        return ccsInfo;
    }

    private CCSFinanceData crawlSingleFinanceData(WebDriver driver) {
        CCSFinanceData financeData = new CCSFinanceData();
        Document doc = Jsoup.parse(driver.getPageSource());

        String intro = HtmlParser.text(doc, ".stock-info-warning");
        financeData.setIntro(StringUtils.trim(intro));

        String currency = HtmlParser.text(doc, ".stock-info-currency");
        financeData.setCurrency(StringUtils.trim(currency));

        String[] timeScopes = new String[] {"全部", "年报", "中报", "一季报", "三季报"};

        for (int i = 0; i < timeScopes.length; i++) {
            String scope = timeScopes[i];
            try {
                WebElement timeElm = driver.findElement(By.cssSelector(".stock-info-btn-list > span:nth-child(" + (i + 1) + ")"));
                PageUtils.click(driver, timeElm);
                WaitTime.Shortest.execute();
            } catch (NoSuchElementException e) {
                logger.error("Unable to find {} on page", scope);
                return null;
            }

            List<String> pageContents = new ArrayList<>();
            while (true) {
                doc = Jsoup.parse(driver.getPageSource());
                Element selectedElm = doc.select("span.btn:contains(" + scope + ")").first();
                if (selectedElm == null || !selectedElm.hasClass("active")) {
                    logger.error("Unable to navigate to page: {}", scope);
                    return null;
                }

                if (HtmlParser.anyExist(doc, ".no-data")) {
                    break;
                }

                Element tableElm = doc.select(".stock-info-content > .tab-table-responsive > table").first();
                if (tableElm == null) {
                    continue;
                }
                pageContents.add(tableElm.outerHtml());

                try {
                    List<WebElement> navElms = driver.findElements(By.cssSelector(".table-operate > span"));
                    WebElement nextElm = navElms.get(navElms.size() - 1);
                    if (StringUtils.isNotBlank(nextElm.getAttribute("disabled"))) {
                        logger.warn("Next page ended.");
                        break;
                    }
                    PageUtils.click(driver, nextElm);
                    WaitTime.Shortest.execute();
                } catch (NoSuchElementException e) {
                    logger.warn("Next page link not found, page ended.");
                    break;
                }
            }
            financeData.assignPageFields(scope, pageContents);
        }

        return financeData;
    }

    /**
     * Crawl all CCS entities from entry page
     */
    private Set<CCSData> crawlList(WebDriver driver) {
        Set<CCSData> ccsDataSet = new HashSet<>();
        while (true) {
            PageUtils.scrollToBottom(driver);
            Document doc = Jsoup.parse(driver.getPageSource());
            this.parseCCSDataFromList(doc, ccsDataSet);
            if (!PageLoadHelper.clickable(driver, By.cssSelector(".next > a"), WaitTime.Shortest)) {
                logger.info("Page ended.");
                break;
            }
            PageUtils.click(driver, By.cssSelector(".next > a"));
            WaitTime.Shortest.execute();
        }
        return ccsDataSet;
    }

    private void parseCCSDataFromList(Document doc, Set<CCSData> ccsDataSet) {
        Elements rows = doc.select("#stockList > .new-portfolio > .portfolio > tbody > tr");
        for (Element row : rows) {
            Elements tds = row.select("td");
            CCSData data = new CCSData();
            data.setStockCode(tds.get(0).text().trim());
            data.setStockName(tds.get(1).text().trim());

            data.setCurrentPrice(this.parseFloat(tds.get(2).text().trim()));
            data.setUpDown(this.parseFloat(tds.get(3).text().trim()));
            data.setUpDownPercent(this.parseFloat(tds.get(4).text().trim()));
            data.setYear2NowPercent(this.parseFloat(tds.get(5).text().trim()));

            String volumeText = tds.get(6).text().trim();
            data.setDealVolume(this.parseFloat(volumeText));
            data.setDealVolumeUnit(this.parseUnit(volumeText));

            String turnoverText = tds.get(7).text().trim();
            data.setTurnover(this.parseFloat(turnoverText));
            data.setTurnoverUnit(this.parseUnit(turnoverText));

            data.setTurnoverRatioPercent(this.parseFloat(tds.get(8).text().trim()));
            data.setPeRatio(this.parseFloat(tds.get(9).text().trim()));
            data.setDividendYieldPercent(this.parseFloat(tds.get(10).text().trim()));

            String marketValueText = tds.get(11).text().trim();
            data.setMarketValue(this.parseFloat(marketValueText));
            data.setMarketValueUnit(this.parseUnit(marketValueText));

            ccsDataSet.add(data);
        }
    }

    private float parseFloat(String text) {
        if (StringUtils.isBlank(text) || StringUtils.equals(text, "-")) {
            return 0;
        }
        String clean = StringUtils.removePattern(text, Regex.NON_CURRENCY.val());
        if (StringUtils.isBlank(clean)) {
            return 0;
        }
        return NumberUtils.toFloat(clean);
    }

    private String parseUnit(String text) {
        if (StringUtils.isBlank(text) || StringUtils.equals(text, "-")) {
            return StringUtils.EMPTY;
        }
        String clean = StringUtils.removePattern(text, "[0-9,.]");
        if (StringUtils.isBlank(clean)) {
            return StringUtils.EMPTY;
        }
        return StringUtils.trim(clean);
    }
}
