package cn.btimes.source;

import cn.btimes.model.common.*;
import cn.btimes.model.common.BTExceptions.ArticleNoImageException;
import cn.btimes.model.common.BTExceptions.PastDateException;
import cn.btimes.service.ApiRequest;
import cn.btimes.service.WebDriverLauncher;
import cn.btimes.utils.Common;
import cn.btimes.utils.PageUtils;
import cn.btimes.utils.Tools;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.amzass.enums.common.DateFormat;
import com.amzass.enums.common.Directory;
import com.amzass.model.common.ActionLog;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Constants;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.amzass.utils.common.RegexUtils;
import com.google.inject.Inject;
import com.kber.commons.DBManager;
import com.mailman.model.common.WebApiResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.HttpHost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Minutes;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 12/24/2018 6:29 PM
 */
public abstract class Source {
    private static final String WITHOUT_YEAR = "1970";
    private static final String WITHOUT_MONTH_DAY = "01/01";
    private static final String GALLERY_TYPOLOGY = "2";
    private final Logger logger = LoggerFactory.getLogger(Source.class);
    private static final String DOWNLOAD_PATH = "downloads";
    private static final List<String[]> sources = readSources();
    private static final String PROXY = Tools.getCustomizingValue("PROXY");
    @Inject private DBManager dbManager;
    @Inject Messengers messengers;
    @Inject private ApiRequest apiRequest;
    private Map<String, String> adminCookie;
    protected Config config;
    Set<Article> relatedArticles = new HashSet<>();
    WebDriver driver;

    protected abstract Map<String, Category> getUrls();

    protected abstract String getDateRegex();

    protected abstract String getDateFormat();

    protected abstract CSSQuery getCSSQuery();

    protected abstract int getSourceId();

    protected abstract List<Article> parseList(Document doc);

    protected abstract void readArticle(WebDriver driver, Article article);

    boolean withoutDriver() {
        return false;
    }

    protected Elements readList(Document doc) {
        String cssQuery = this.getCSSQuery().getList();
        this.checkArticleListExistence(doc, cssQuery);
        return doc.select(cssQuery);
    }

    protected void readContent(WebDriver driver, Article article) {
        Document doc = this.openArticlePage(driver, article);
        this.parseContent(doc, article);
    }

    protected void readDateSummaryContent(WebDriver driver, Article article) {
        Document doc = this.openArticlePage(driver, article);

        this.parseDate(doc, article);
        this.parseSummary(doc, article);
        this.parseContent(doc, article);
    }

    void readSummaryContent(WebDriver driver, Article article) {
        Document doc = this.openArticlePage(driver, article);

        this.parseSummary(doc, article);
        this.parseContent(doc, article);
    }

    protected void readTitleContent(WebDriver driver, Article article) {
        Document doc = this.openArticlePage(driver, article);

        this.parseTitle(doc, article);
        this.parseContent(doc, article);
    }

    void readTitleDateContent(WebDriver driver, Article article) {
        Document doc = this.openArticlePage(driver, article);

        this.parseTitle(doc, article);
        this.parseDate(doc, article);
        this.parseContent(doc, article);
    }

    protected void readDateContent(WebDriver driver, Article article) {
        Document doc = this.openArticlePage(driver, article);

        this.parseDate(doc, article);
        this.parseContent(doc, article);
    }

    Document openArticlePage(WebDriver driver, Article article) {
        return this.openPage(driver, article.getUrl(), false);
    }

    private Document openPage(WebDriver driver, String url, boolean loadMore) {
        if (StringUtils.isBlank(url)) {
            return new Document("");
        }
        if (this.withoutDriver()) {
            try {
                return cn.btimes.utils.PageUtils.getDocumentByJsoup(url);
            } catch (BusinessException e) {
                logger.error("Unable to load page via Jsoup, try using WebDriver: {}", url);
            }
        }
        try {
            driver.get(url);
        } catch (TimeoutException e) {
            logger.warn("List page loading timeout, try ignoring the exception: {}", url);
        }

        // Scroll to bottom to make sure latest content are loaded
        PageUtils.scrollToBottom(driver);
        if (loadMore) {
            this.loadMoreList(driver);
        }
        WaitTime.Normal.execute();

        return Jsoup.parse(driver.getPageSource());
    }

    void loadMoreList(WebDriver driver) {

    }

    protected void parseDateTitleSummaryList(List<Article> articles, Elements list) {
        int i = 0;
        for (Element row : list) {
            try {
                Article article = new Article();
                this.parseDate(row, article);
                this.parseTitle(row, article);
                this.parseSummary(row, article);

                articles.add(article);
            } catch (PastDateException e) {
                if (i++ < Constants.MAX_REPEAT_TIMES) {
                    continue;
                }
                logger.warn("Article that past {} minutes detected, complete the list fetching: ",
                    config.getMaxPastMinutes(), e);
                break;
            }
        }
    }

    protected void parseTitleList(List<Article> articles, Elements list) {
        for (Element row : list) {
            Article article = new Article();
            this.parseTitle(row, article);
            this.parseCover(row, article);
            articles.add(article);
        }
    }

    Article parseDateTitleWithTimeText(Element row) {
        String dateTextCssQuery = this.getCSSQuery().getTime();
        this.checkDateTextExistence(row, dateTextCssQuery);
        String timeText = HtmlParser.text(row, dateTextCssQuery);
        if (!cn.btimes.utils.Tools.containsAny(timeText, "小时", "分", "秒", "今")) {
            logger.warn("Skip past date article: {}", timeText);
            return null;
        }

        Article article = new Article();
        this.parseTitle(row, article);
        this.parseCover(row, article);
        return article;
    }

    protected void parseDateTitleList(List<Article> articles, Elements list) {
        int i = 0;
        for (Element row : list) {
            try {
                Article article = new Article();
                this.parseDate(row, article);
                this.parseTitle(row, article);
                this.parseCover(row, article);

                articles.add(article);
            } catch (PastDateException e) {
                if (this.ignorePastDateException()) {
                    continue;
                }
                if (i++ < Constants.MAX_REPEAT_TIMES) {
                    continue;
                }
                logger.warn("Article that past {} minutes detected, complete the list fetching: ",
                    config.getMaxPastMinutes(), e);
                break;
            }
        }
    }

    boolean ignorePastDateException() {
        return false;
    }

    public void parseTitle(Element doc, Article article) {
        CSSQuery cssQuery = this.getCSSQuery();
        this.checkTitleExistence(doc, cssQuery.getTitle());
        Element linkElm = doc.select(cssQuery.getTitle()).first();
        if (linkElm == null) {
            throw new BusinessException("Unable to find article title.");
        }
        if (StringUtils.isBlank(article.getUrl())) {
            article.setUrl(linkElm.attr("href"));
        }
        article.setTitle(linkElm.text());
    }

    protected void parseSummary(Element doc, Article article) {
        CSSQuery cssQuery = this.getCSSQuery();
        if (StringUtils.isBlank(cssQuery.getSummary())) {
            return;
        }
        this.checkSummaryExistence(doc, cssQuery.getSummary());
        String source = HtmlParser.text(doc, cssQuery.getSummary());
        article.setSummary(source);
    }

    protected void parseContent(Document doc, Article article) {
        this.checkContent(doc);

        CSSQuery cssQuery = this.getCSSQuery();
        this.checkArticleContentExistence(doc, cssQuery.getContent());
        Elements contentElms = doc.select(cssQuery.getContent());
        if (contentElms.size() == 0) {
            throw new BusinessException(String.format("Content not found with selector: %s", cssQuery.getContent()));
        }
        Element contentElm = contentElms.first();
        article.setContent(this.cleanHtml(contentElm));
        this.parseCoverImageFromContent(doc, article);
        this.fetchContentImages(article);
    }

    void checkContent(Document doc) {
        this.removeDomNotInContentArea(doc);
        String html = doc.html();
        if (Tools.containsAny(html, "本文", "转自", "源自", "转载", "来自", "证券时报")) {
            throw new BusinessException("Article is skipped due to contains unsupported keywords.");
        }
    }

    void removeDomNotInContentArea(Document doc) {

    }

    void parseCoverImageFromContent(Document doc, Article article) {
    }

    boolean allowArticleWithoutImage() {
        return true;
    }

    void parseCover(Element row, Article article) {
        String coverSelector = this.getCoverSelector();
        if (StringUtils.isBlank(coverSelector)) {
            return;
        }

        Element imageElm = row.select(coverSelector).first();
        if (imageElm == null) return;
        String src = imageElm.attr("src");
        article.setCoverImage(Common.getAbsoluteUrl(src, driver.getCurrentUrl()));
    }

    String getCoverSelector() {
        return null;
    }

    void initContext(Config config) {
        this.adminCookie = WebDriverLauncher.adminCookies.get(config.getApplication());
        this.config = config;
    }

    public void execute(WebDriver driver, Config config) {
        this.initContext(config);
        this.driver = driver;
        this.relatedArticles.clear();
        List<Article> articles = new ArrayList<>();
        Map<String, Category> urls = this.getUrls();
        for (String url : urls.keySet()) {
            Document doc = this.openPage(driver, url, true);
            try {
                List<Article> articlesNew = this.parseList(doc);
                int size = articlesNew.size();
                Category category = urls.get(url);
                if (size == 0) {
                    String fileName = this.getClass().getSimpleName() + System.currentTimeMillis();
                    logger.warn(fileName + ": " + url);
                    PageUtils.savePage4ErrorHandling(driver, fileName, "parseList");
                    continue;
                }
                logger.info("Found {} articles in {}", size, category.name());

                articlesNew.forEach(article -> {
                    article.setCategory(category);
                    article.setUrl(Common.getAbsoluteUrl(article.getUrl(), url));
                });
                articles.addAll(articlesNew);
            } catch (BusinessException e) {
                logger.error("Error found in parsing list, skip current list: ", e);
            }
        }

        articles = this.mergeRelatedArticles(articles);

        logger.info("Found {} articles from list.", articles.size());
        if (articles.size() == 0) {
            PageUtils.savePage4ErrorHandling(driver, this.getClass().getSimpleName() + System.currentTimeMillis(), "All");
        }

        int saved = 0;
        for (Article article : articles) {
            String logId;
            String logIdTitle = null;
            if (article.getId() == 0) {
                logId = Common.toMD5(article.getUrl());
                logIdTitle = Common.toMD5(this.getClass() + article.getTitle());
                ActionLog log = dbManager.readById(logId, ActionLog.class);

                if (log == null) {
                    log = dbManager.readById(logIdTitle, ActionLog.class);
                }
                if (log != null) {
                    logger.info("Article saved already: {} -> {}", article.getTitle(), article.getUrl());
                    continue;
                }
            } else {
                logId = "ArticleImage_" + article.getId();
                ActionLog log = dbManager.readById(logId, ActionLog.class);
                if (log != null) {
                    logger.debug("Article doesn't contain image: {} -> {}", article.getTitle(), article.getId());
                    continue;
                }
            }
            try {
                this.readArticle(driver, article);
                this.saveArticle(article, driver);
                saved++;
            } catch (ArticleNoImageException e) {
                logger.error(e.getMessage() + ": " + article.getUrl(), e);
                if (article.getId() > 0) {
                    dbManager.save(new ActionLog(logId), ActionLog.class);
                }
            } catch (PastDateException e) {
                logger.error("Article publish date has past {} minutes: {}",
                    config.getMaxPastMinutes(), article.getUrl(), e);
                if (article.getId() == 0) {
                    dbManager.save(new ActionLog(logId), ActionLog.class);
                    dbManager.save(new ActionLog(logIdTitle), ActionLog.class);
                }
            } catch (BusinessException e) {
                String message = String.format("Unable to read article %s", article.getUrl());
                logger.error(message, e);
                Messenger messenger = new Messenger(this.getClass().getName(), message + ": " + e.getMessage());
                this.messengers.add(messenger);
            } catch (TimeoutException e) {
                throw e;
            } catch (Exception e) {
                String message = String.format("Exception found for article: %s -> %s", article.getTitle(), article.getUrl());
                logger.error(message, e);

                Messenger messenger = new Messenger(this.getClass().getName(), message + ": " + e.getMessage());
                this.messengers.add(messenger);
                continue;
            }
            if (article.getId() == 0) {
                dbManager.save(new ActionLog(logId), ActionLog.class);
                dbManager.save(new ActionLog(logIdTitle), ActionLog.class);
            }
        }

        this.saveRelatedArticles();

        if (articles.size() > 0) {
            Messenger messenger = new Messenger(this.getClass().getName(),
                String.format("%d of %d articles are saved.", saved, articles.size()));
            this.messengers.add(messenger);
        }
    }

    private File getRelatedArticlesFile() {
        String fileName = String.format("related/%s.json", this.getClass().getSimpleName());
        return FileUtils.getFile(fileName);
    }

    private void saveRelatedArticles() {
        if (this.relatedArticles.size() == 0) {
            return;
        }
        Tools.writeStringToFile(this.getRelatedArticlesFile(), JSON.toJSONString(this.relatedArticles));
        this.relatedArticles.clear();
    }

    private List<Article> mergeRelatedArticles(List<Article> articles) {
        File relatedFile = this.getRelatedArticlesFile();
        if (!relatedFile.exists()) {
            return articles;
        }
        String text = Tools.readFileToString(relatedFile);
        List<Article> articleList = JSON.parseArray(text, Article.class);
        if (articleList.size() == 0) {
            return articles;
        }
        articles.addAll(articleList);
        Set<Article> articleSet = new HashSet<>(articles);
        relatedFile.deleteOnExit();
        return new ArrayList<>(articleSet);
    }

    void saveArticle(Article article, WebDriver driver) {
        article.validate();
        if (article.hasImages()) {
            ImageUploadResult result = this.uploadImages(article, driver);
            if (result != null && result.hasFiles()) {
                List<SavedImage> savedImages = this.saveImages(article, result.getFiles());
                this.deleteDownloadedImages(savedImages);
                this.replaceImages(article, savedImages);
            } else if (!this.allowArticleWithoutImage() || article.getId() > 0) {
                throw new ArticleNoImageException("This article is skipped due to having no images: " + article.getTitle());
            }
        }
        this.cleanThirdPartyImages(article);
        article.validate();

        if (article.getId() > 0) {
            this.updateArticle(article);
            logger.info("Article updated: {} -> {}", article.getTitle(), article.getId());
            return;
        }

        int cateId = config.getCategories().get(article.getCategory());
        Connection conn = this.createWebConnection(config.getArticleSaveUrl(), adminCookie)
            .data("getstring", "")
            .data("mb_no", "")
            .data("ar_status", this.getStatus())
            .data("ar_status_old", "")
            .data("ar_copy_edited_finished", "")
            .data("use_article_all_table", "")
            .data("ar_title", article.getTitle())
            .data("ar_mtitle", "")
            .data("ar_summary", article.getSummary())
            .data("ar_content", article.getContent())
            .data("tex", "")
            .data("ar_cat[]", String.valueOf(cateId))
            .data("ar_keyword", "")
            .data("ar_newskeyword", article.getTitle())
            .data("ar_youtube", "")
            .data("ar_typology", this.getTypology())
            .data("ar_tag", "")
            .data("ar_topic", "")
            .data("ar_related", "")
            .data("arDate", (new SimpleDateFormat("MM/dd/YYYY HH:mm")).format(new Date()))
            .data("ar_reporter", "")
            .data("ar_reporter_email", "")
            .data("ar_originlink", article.getUrl())
            .data("ar_source", this.determineSource(article.getSource(), this.getSourceId()));
        if (article.hasImageIds()) {
            StringBuilder sb = new StringBuilder();

            int length = article.getImageIds().length;
            for (int i = 0; i < length; i++) {
                String imageId = String.valueOf(article.getImageIds()[i]);
                if (!GALLERY_TYPOLOGY.equals(this.getTypology()) && article.isHideTopImages()) {
                    conn.data("imghidden_" + imageId, "")
                        .data("ar_image_hide[" + i + "]", imageId);
                }
                sb.append(imageId);
                if (i != length - 1) {
                    sb.append(Constants.COMMA);
                }
            }
            conn.data("ar_image", sb.toString());
        }
        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            try {
                String articleId = conn.execute().body();
                if (RegexUtils.match(articleId, "\\d+")) {
                    logger.info("Article saved: {} -> {}", article.getTitle(), articleId);
                    return;
                } else {
                    String message = String.format("Article saved failed: %s", article.getTitle());
                    logger.error(message);
                    Messenger messenger = new Messenger(this.getClass().getName(), message);
                    this.messengers.add(messenger);
                }
            } catch (IOException e) {
                String message = "Unable to save the article:";
                logger.error(message, e);
                Messenger messenger = new Messenger(this.getClass().getName(), message + e.getMessage());
                this.messengers.add(messenger);
            }
        }
        throw new BusinessException(String.format("Unable to save the article nb v [%s]%s -> %s",
            article.getSource(), article.getTitle(), article.getUrl()));
    }

    private void updateArticle(Article article) {
        String dataText = JSON.toJSONString(article);
        WebApiResult result = apiRequest.post("/article/updateArticleImages", dataText, config);
        System.out.println(result);
    }

    String getStatus() {
        return "1";
    }

    String getTypology() {
        return "1";
    }

    private void checkArticleListExistence(Element doc, String cssQuery) {
        this.checkElementExistence(doc, cssQuery, "Article list");
    }

    void checkArticleContentExistence(Element doc, String cssQuery) {
        this.checkElementExistence(doc, cssQuery, "Article content");
    }

    protected void checkDateTextExistence(Element doc, String cssQuery) {
        this.checkElementExistence(doc, cssQuery, "Date text");
    }

    protected void checkTitleExistence(Element doc, String cssQuery) {
        this.checkElementExistence(doc, cssQuery, "Title");
    }

    private void checkSummaryExistence(Element doc, String cssQuery) {
        this.checkElementExistence(doc, cssQuery, "Summary");
    }

    private void checkElementExistence(Element doc, String cssQuery, String name) {
        if (HtmlParser.anyExist(doc, cssQuery)) {
            return;
        }
        Messenger messenger = new Messenger(this.getClass().getName(),
            String.format("%s not found with: %s", name, cssQuery));
        this.messengers.add(messenger);
    }

    private void deleteDownloadedImages(List<SavedImage> savedImages) {
        for (SavedImage image : savedImages) {
            FileUtils.deleteQuietly(FileUtils.getFile(image.getPath()));
        }
    }

    void fetchContentImages(Article article) {
        Document doc = Jsoup.parse(article.getContent());
        Elements images = doc.select("img");
        List<Image> contentImages = article.getContentImages();
        for (Element image : images) {
            String srcAttr = this.getContentImageSrcAttr();
            String src = image.attr(srcAttr);
            if (StringUtils.equalsIgnoreCase(srcAttr, "data-src")) {
                image.attr("src", src);
            }
            image.removeAttr("crossorigin");
            image.removeAttr("data-src");

            if (StringUtils.containsIgnoreCase(src, "data:image") ||
                StringUtils.containsIgnoreCase(src, "base64") ||
                StringUtils.containsIgnoreCase(src, "/data/images") ||
                Tools.containsAny(src, "businesstimes")) {
                continue;
            }
            String absSrc = Common.getAbsoluteUrl(src, article.getUrl());
            image.attr("src", absSrc);

            Image imageObj = new Image(absSrc, "");
            contentImages.add(imageObj);
        }
        String cover = article.getCoverImage();
        if (contentImages.size() == 0 && !this.allowArticleWithoutImage()) {
            if (StringUtils.isNotBlank(cover)) {
                Image imageObj = new Image(cover, "");
                contentImages.add(imageObj);
            } else {
                String message = String.format("This article is skipped due to having no images: %d -> %s",
                    article.getId(), article.getTitle());
                throw new ArticleNoImageException(message);
            }
        }
        article.setContent(doc.select("body").html());
        article.setContentImages(contentImages);
    }

    String getContentImageSrcAttr() {
        return "src";
    }

    protected void resizeContentImage(Article article, String[] from, String[] to) {
        String content = article.getContent();

        List<Image> images = article.getContentImages();
        Iterator<Image> it = images.iterator();
        while (it.hasNext()) {
            Image image = it.next();
            String newUrl = image.getUrl();
            for (int i = 0; i < from.length; i++) {
                newUrl = StringUtils.replacePattern(newUrl, from[i], to[i]);
            }
            content = StringUtils.replace(content, image.getUrl(), newUrl);
            image.setUrl(newUrl);
        }

        article.setContent(content);
        article.setContentImages(images);
    }

    protected void parseDate(Element doc, Article article) {
        CSSQuery cssQuery = this.getCSSQuery();
        this.checkDateTextExistence(doc, cssQuery.getTime());
        String timeText = HtmlParser.text(doc, cssQuery.getTime());
        article.setDate(this.parseDateText(timeText));
    }

    Date parseDescribableDateText(String timeText) {
        if (Tools.containsAny(timeText, "刚刚")) {
            return new Date();
        }
        int minutes = NumberUtils.toInt(RegexUtils.getMatched(timeText, "\\d+"));
        if (minutes == 0) {
            throw new BusinessException("Unable to parse time text: " + timeText);
        }
        if (Tools.contains(timeText, "小时")) {
            minutes *= 60;
        } else if (!Tools.contains(timeText, "分")) {
            throw new PastDateException("Time text doesn't contain minutes or hours: " + timeText);
        }

        if (minutes > config.getMaxPastMinutes()) {
            throw new PastDateException("Time has past limit: " + timeText);
        }
        return DateUtils.addMinutes(new Date(), -1 * minutes);
    }

    protected Date parseDateText(String timeText) {
        return this.parseDateText(timeText, this.getDateRegex(), this.getDateFormat());
    }

    private Date parseDateText(String timeText, String regex, String dateFormat) {
        String timeTextClean = RegexUtils.getMatched(timeText, regex);
        try {
            Date date = DateUtils.parseDate(timeTextClean, Locale.PRC, dateFormat);
            date = this.fixDate(date);

            this.checkDate(date);
            return date;
        } catch (ParseException e) {
            throw new PastDateException(String.format("Unable to parse date: %s -> %s", timeText, timeTextClean));
        } catch (PastDateException e) {
            throw new PastDateException(String.format("Time has past limit: %s -> %s.", timeText, timeTextClean));
        }
    }

    private Date fixDate(Date date) {
        // If dateFormat without year, set as current year
        if (DateFormat.YEAR.format(date).equals(WITHOUT_YEAR)) {
            int year = Calendar.getInstance().get(Calendar.YEAR);
            date = DateUtils.setYears(date, year);
        }
        // If dateFormat without month and day, set today
        if (StringUtils.equals(DateFormat.FULL_MONTH_DAY.format(date), WITHOUT_MONTH_DAY)) {
            int month = Calendar.getInstance().get(Calendar.MONTH);
            int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
            date = DateUtils.setMonths(date, month);
            date = DateUtils.setDays(date, day);
        }
        return date;
    }

    void checkDate(Date date) {
        if (this.calcMinutesAgo(date) > config.getMaxPastMinutes()) {
            throw new PastDateException();
        }
    }

    protected Date parseDateTextWithDay(String timeText, String regex, String dateFormat, int maxPastDays) {
        try {
            timeText = RegexUtils.getMatched(timeText, regex);
            Date date = DateUtils.parseDate(timeText, Locale.PRC, dateFormat);
            date = this.fixDate(date);
            if (Days.daysBetween(new DateTime(date), DateTime.now()).getDays() > maxPastDays) {
                throw new PastDateException();
            }
            return date;
        } catch (ParseException e) {
            logger.error(String.format("Unable to parse date: %s", timeText));
            throw new PastDateException();
        }
    }

    private void cleanThirdPartyImages(Article article) {
        if (!article.hasImages()) {
            return;
        }
        Element dom = Jsoup.parse(article.getContent()).body();
        boolean hasRemoval = false;
        for (Element image : dom.select("img")) {
            String src = image.attr("src");
            if (StringUtils.startsWithIgnoreCase(src, "/data/images") ||
                StringUtils.containsIgnoreCase(src, config.getCdnUrl())) {
                this.justifyImage(image);
                continue;
            }
            image.remove();
            hasRemoval = true;
        }
        if (hasRemoval) {
            this.removeNeedlessHtmlTags(dom);
        }
        article.setContent(dom.select("body").html());
    }

    private void justifyImage(Element image) {
        Element parent = image.parent();
        if (!this.hasText(parent)) {
            parent.tagName("div");
            Set<String> classNames = new HashSet<>();
            classNames.add("imageBox");
            parent.classNames(classNames);
            return;
        }
        image.after("<div class=\"imageBox\"><img src=\"" + image.attr("src") + "\" /></div>");
        image.remove();
    }

    private String determineSource(String source, int sourceId) {
        if (StringUtils.isNotBlank(source)) {
            for (String[] sourcePair : sources) {
                if (Tools.contains(sourcePair[1], source)) {
                    return sourcePair[0];
                }
            }
        }
        return String.valueOf(sourceId);
    }

    /**
     * Create web connection to admin site
     */
    private Connection createWebConnection(String url, Map<String, String> cookies) {
        Connection conn = Jsoup.connect(config.getAdminUrl() + url)
            .validateTLSCertificates(false)
            .userAgent("Mozilla")
            .method(Method.POST)
            .data("maxFileNum", "50")
            .data("maxFileSize", "200 MB")
            .data("unique_key", Common.toMD5(String.valueOf(System.currentTimeMillis())))
            .data("field", "image_hidden")
            .data("func", "photo_image_content")
            .data("request_from", "ContentCrawler")
            .timeout(0).maxBodySize(0);
        if (cookies != null) {
            conn.cookies(cookies);
        }
        return conn;
    }

    private void replaceImages(Article article, List<SavedImage> savedImages) {
        String content = article.getContent();
        int[] imageIds = new int[savedImages.size()];
        for (Image image : article.getContentImages()) {
            String hex = Common.toMD5(image.getUrl());
            for (SavedImage savedImage : savedImages) {
                if (StringUtils.startsWith(savedImage.getOriginalFile(), hex)) {
                    String newImage = config.getDataImagesFull() + savedImage.getPath();
                    content = StringUtils.replace(content, image.getUrl(), newImage);
                    break;
                }
            }
        }
        article.setContent(content);

        int i = 0;
        for (SavedImage savedImage : savedImages) {
            imageIds[i++] = savedImage.getImageId();
        }
        article.setImageIds(imageIds);
    }

    /**
     * Download the images from article source,
     * then upload to server temp directory
     */
    private ImageUploadResult uploadImages(Article article, WebDriver driver) {
        if (!article.hasImages()) {
            return null;
        }
        Connection conn = this.createWebConnection(config.getFileUploadUrl(), adminCookie);

        int i = 0;
        for (Image contentImage : article.getContentImages()) {
            DownloadResult downloadResult;
            try {
                downloadResult = this.downloadFile(contentImage.getUrl());
            } catch (BusinessException e) {
                String message = String.format("Unable to download image: %s", contentImage.getUrl());
                logger.error(message);
                Messenger messenger = new Messenger(this.getClass().getName(), message + ": " + e.getMessage());
                this.messengers.add(messenger);
                continue;
            }
            String image = downloadResult.getFullPath();
            File file = FileUtils.getFile(image);
            try {
                FileInputStream fs = new FileInputStream(file);
                conn.data("files[" + i + "]", image, fs);
                i++;
            } catch (IOException e) {
                String message = String.format("Unable to download file: %s", image);
                logger.error(message, e);
                Messenger messenger = new Messenger(this.getClass().getName(), message + ": " + e.getMessage());
                this.messengers.add(messenger);
            }
        }
        if (i == 0) {
            logger.error("No files downloaded.");
            return null;
        }
        String message = null;
        for (int j = 0; j < Constants.MAX_REPEAT_TIMES; j++) {
            try {
                String body = conn.execute().body();
                ImageUploadResult result = JSONObject.parseObject(body, ImageUploadResult.class);
                if (!result.hasFiles()) {
                    message = String.format("Files are not uploaded, retry uploading: %s", body);
                    logger.error(message);
                    continue;
                }
                return result;
            } catch (Exception e) {
                message = String.format("Unable to upload files, retry in %d seconds:", WaitTime.Normal.val());
                logger.error(message, e);
                WaitTime.Normal.execute();
            }
        }
        Messenger messenger = new Messenger(this.getClass().getName(), String.format("Unable to upload file: %s", message));
        this.messengers.add(messenger);
        return null;
    }

    /**
     * Save uploaded images to DB, and move out of the temp directory
     */
    private List<SavedImage> saveImages(Article article, List<UploadedImage> files) {
        Connection conn = this.createWebConnection(config.getFileSaveUrl(), adminCookie);
        List<Image> contentImages = article.getContentImages();

        int i = 0;
        for (UploadedImage imageFile : files) {
            this.assignImageContent(imageFile, contentImages);
            conn.data("im_title[" + i + "]", article.getTitle())
                .data("im_content[" + i + "]", imageFile.getContent())
                .data("im_credit[" + i + "]", article.getSource())
                .data("im_link[" + i + "]", imageFile.getImLink())
                .data("im_reporter[" + i + "]", article.getReporter())
                .data("im_x_pos[" + i + "]", "50")
                .data("im_y_pos[" + i + "]", "40")
                .data("uploadedFile[" + i + "]", imageFile.getName())
                .data("originalFile[" + i + "]", imageFile.getOriginalFile());
            i++;
        }

        for (int j = 0; j < Constants.MAX_REPEAT_TIMES; j++) {
            try {
                String body = conn.execute().body();
                return JSONObject.parseArray(body, SavedImage.class);
            } catch (IOException e) {
                String message = String.format("Unable to save the files, retry in %d seconds:", WaitTime.Normal.val());
                logger.error(message, e);
                Messenger messenger = new Messenger(this.getClass().getName(), message + ": " + e.getMessage());
                this.messengers.add(messenger);
                WaitTime.Normal.execute();
            }
        }
        return null;
    }

    private void assignImageContent(UploadedImage imageFile, List<Image> contentImages) {
        for (Image image : contentImages) {
            if (StringUtils.startsWithIgnoreCase(imageFile.getOriginalFile(), Common.toMD5(image.getUrl()))) {
                imageFile.setContent(image.getContent());
                imageFile.setImLink(image.getUrl());
                return;
            }
        }
    }

    /**
     * Download image from its url
     */
    private DownloadResult downloadFile(String url) {
        long start = System.currentTimeMillis();
        String originalUrl = url;
        url = Common.getAbsoluteUrl(url, driver.getCurrentUrl());
        String fileName = Common.extractFileNameFromUrl(url);
        File file = this.makeDownloadFile(fileName);
        String path = file.getAbsolutePath();
        String prefix = Tools.startWithAny(url, Constants.HTTP) ? StringUtils.EMPTY : Constants.HTTP + ":";
        url = prefix + url;

        file = Tools.downloadFile(url, path);
        String size = FileUtils.byteCountToDisplaySize(file.length());
        if (file.length() <= 100) {
            throw new BusinessException(String.format("Image download is skipped due to too small: %s -> %s.",
                file.length(), url));
        }
        try {
            DownloadResult result = this.makeDownloadResult(url, originalUrl, path);
            logger.info("{} file downloaded, time costs {}. Size:{}, path:{}",
                result.getFileName(),
                Tools.formatCostTime(start),
                size, result.getFullPath());
            return result;
        } catch (IOException e) {
            logger.error("Unable to download file {}", url);
        } finally {
            file.deleteOnExit();
        }

        throw new BusinessException(String.format("Failed to execute %s file download request after retried.", fileName));
    }

    private CloseableHttpClient initHttpClient() {
        if (StringUtils.isBlank(PROXY)) {
            return HttpClients.createDefault();
        }
        logger.info("Using proxy in http request: {}", PROXY);
        String[] hostPort = StringUtils.split(PROXY, ":");
        HttpHost proxy = new HttpHost(hostPort[0], NumberUtils.toInt(hostPort[1]));
        return HttpClientBuilder.create().setProxy(proxy).build();
    }

    private File makeDownloadFile(String fileName) {
        File file = new File(DOWNLOAD_PATH, fileName);
        if (file.exists()) {
            FileUtils.deleteQuietly(file);
            file = new File(DOWNLOAD_PATH, fileName);
        }
        if (!file.canWrite()) {
            file.setWritable(true);
        }
        return file;
    }

    /**
     * Generate new name with MD5 to avoid duplicated image names in an article
     */
    private DownloadResult makeDownloadResult(String url, String originalUrl, String path) throws IOException {
        ImageType type = Common.determineImageFileType(path);
        if (!type.allowed()) {
            if (type == ImageType.WEBP) {
                type = ImageType.PNG;
                Common.convertImageFileTypeFromUrl(url, path, type);
            } else {
                type = ImageType.DEFAULT_TYPE;
                Common.convertImageFileType(path, type);
            }
        }
        String fileNameNew = Common.toMD5(originalUrl) + "." + type.toExt();

        File fileNew = this.makeDownloadFile(fileNameNew);
        if (fileNew.exists()) {
            String fileNewPath = fileNew.getAbsolutePath();
            FileUtils.deleteQuietly(fileNew);
            fileNew = FileUtils.getFile(fileNewPath);
        }
        FileUtils.moveFile(FileUtils.getFile(path), fileNew);
        path = fileNew.getAbsolutePath();

        return new DownloadResult(originalUrl, path, fileNameNew);
    }

    private static List<String[]> readSources() {
        List<String> sourceList = Tools.readFile(FileUtils.getFile(Directory.Customize.path(), "sources.txt"));
        List<String[]> sources = new ArrayList<>();
        for (String sourceText : sourceList) {
            if (StringUtils.isBlank(sourceText)) {
                continue;
            }
            String[] parts = StringUtils.split(sourceText, ":");
            if (parts.length != 2) {
                continue;
            }
            sources.add(parts);
        }
        return sources;
    }

    protected String cleanHtml(Element dom) {
        this.removeNeedlessHtmlTags(dom);
        // this.unwrapDeepLayeredHtmlTags(dom);
        this.removeImgTagAttrs(dom);
        return this.removeHtmlComments(dom.html());
    }

    private void unwrapDeepLayeredHtmlTags(Element dom) {
        Elements elements = dom.children();
        int size = elements.size();
        if (size == 0) {
            return;
        }
        for (Element row : elements) {
            this.unwrapParent(row);
        }
    }

    private void unwrapParent(Element element) {
        Element parent = element.parent();
        int size = parent.children().size();
        if (element.children().size() == 0 && size == 1 &&
            StringUtils.equals(parent.text().trim(), element.text().trim())) {
            parent.after(element.outerHtml());
            parent.remove();
            this.unwrapDeepLayeredHtmlTags(parent);
            return;
        }
        this.unwrapDeepLayeredHtmlTags(element);
    }

    private String removeHtmlComments(String html) {
        List<String> list = RegexUtils.getMatchedList(html, "<\\!--.*-->");
        for (String str : list) {
            html = StringUtils.trim(StringUtils.remove(html, str));
        }
        return html;
    }

    static int extractFromHoursBefore(String timeText) {
        String hoursAgo = "小时前";
        String hours = RegexUtils.getMatched(timeText, "(\\d+)" + hoursAgo);
        hours = StringUtils.remove(hours, hoursAgo);
        return StringUtils.isBlank(hours) ? 9999 : NumberUtils.toInt(hours);
    }

    boolean checkHoursBefore(String timeText) {
        int hours = extractFromHoursBefore(timeText);
        return hours <= config.getMaxPastHours();
    }

    int calcMinutesAgo(Date date) {
        return Minutes.minutesBetween(new DateTime(date), DateTime.now()).getMinutes();
    }

    private void removeNeedlessHtmlTags(Element dom) {
        if (dom == null) {
            return;
        }
        Elements elements = dom.children();
        if (elements.size() == 0) {
            return;
        }

        for (Element element : elements) {
            String tagName = element.tagName();
            if (this.isImageOrBreak(tagName)) {
                continue;
            }
            if (!this.hasContent(element)) {
                element.remove();
                continue;
            }

            // Remove tag attributes
            Attributes attributes = element.attributes();
            if (attributes.size() > 0) {
                for (Attribute attr : attributes) {
                    element.removeAttr(attr.getKey());
                }
            }

            if (!this.isAllowedTag(tagName)) {
                if (this.isBlockTag(tagName)) {
                    // Replace tag names to p
                    element.tagName("p");
                } else {
                    element.tagName("span");
                }
            }
            this.removeNeedlessHtmlTags(element);
        }
    }

    private boolean isAllowedTag(String tagName) {
        return StringUtils.equalsIgnoreCase(tagName, "p") ||
            StringUtils.equalsIgnoreCase(tagName, "span") ||
            StringUtils.equalsIgnoreCase(tagName, "table") ||
            StringUtils.equalsIgnoreCase(tagName, "tr") ||
            StringUtils.equalsIgnoreCase(tagName, "th") ||
            StringUtils.equalsIgnoreCase(tagName, "tr") ||
            StringUtils.equalsIgnoreCase(tagName, "td") ||
            StringUtils.equalsIgnoreCase(tagName, "thead") ||
            StringUtils.equalsIgnoreCase(tagName, "tfoot") ||
            StringUtils.equalsIgnoreCase(tagName, "ul") ||
            StringUtils.equalsIgnoreCase(tagName, "ol") ||
            StringUtils.equalsIgnoreCase(tagName, "li") ||
            StringUtils.equalsIgnoreCase(tagName, "dl") ||
            StringUtils.equalsIgnoreCase(tagName, "dt") ||
            StringUtils.equalsIgnoreCase(tagName, "dd");
    }

    private boolean isBlockTag(String tagName) {
        return StringUtils.equalsIgnoreCase(tagName, "div") ||
            StringUtils.equalsIgnoreCase(tagName, "h2") ||
            StringUtils.equalsIgnoreCase(tagName, "h3") ||
            StringUtils.equalsIgnoreCase(tagName, "h4");
    }

    private boolean hasContent(Element element) {
        return this.hasText(element) ||
            element.select("img").size() > 0;
    }

    private boolean hasText(Element element) {
        return StringUtils.isNotBlank(StringUtils.removePattern(element.text(), "\\s*|\t|\r|\n"));
    }

    private boolean isImageOrBreak(String tagName) {
        return StringUtils.equalsIgnoreCase(tagName, "img") ||
            StringUtils.equalsIgnoreCase(tagName, "br");
    }

    private void removeImgTagAttrs(Element dom) {
        if (dom == null) {
            return;
        }
        Elements images = dom.select("img");
        if (images.size() == 0) {
            return;
        }
        images.removeAttr("width")
            .removeAttr("height")
            .removeAttr("class")
            .removeAttr("style")
            .removeAttr("srcset")
            .removeAttr("origin");
    }
}
