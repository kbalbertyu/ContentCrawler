package cn.btimes.source;

import cn.btimes.model.*;
import cn.btimes.model.BTExceptions.PastDateException;
import cn.btimes.service.WebDriverLauncher;
import cn.btimes.utils.Common;
import com.alibaba.fastjson.JSONObject;
import com.amzass.enums.common.DateFormat;
import com.amzass.enums.common.Directory;
import com.amzass.model.common.ActionLog;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.*;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.google.inject.Inject;
import com.kber.commons.DBManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
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
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 12/24/2018 6:29 PM
 */
public abstract class Source {
    private static final String WITHOUT_YEAR = "1970";
    private static final String WITHOUT_MONTH_DAY = "01/01";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String DOWNLOAD_PATH = "downloads";
    public static final String ADMIN_URL = Tools.getCustomizingValue("ADMIN_URL");
    private static final String FILE_UPLOAD_URL = ADMIN_URL + "/plugin/jqueryfileupload/server/php/";
    private static final String FILE_SAVE_URL = ADMIN_URL + "/pages/media/plugin/image_upload_multi_update.php";
    private static final String ARTICLE_SAVE_URL = ADMIN_URL + "/pages/publish/publish/update.php";
    private static final String CDN_URL = Tools.getCustomizingValue("CDN_URL");
    private static final String DATA_IMAGES_FULL = "/data/images/full/";
    static final int MAX_PAST_MINUTES = NumberUtils.toInt(Tools.getCustomizingValue("MAX_PAST_MINUTES"));
    private static final List<String[]> sources = readSources();
    @Inject private DBManager dbManager;

    protected abstract Map<String, Category> getUrls();

    protected abstract List<Article> parseList(Document doc);

    protected abstract Boolean validateLink(String href);

    protected abstract void readArticle(WebDriver driver, Article article);

    protected abstract Date parseDateText(String timeText);

    protected abstract Date parseDate(Document doc);

    protected abstract void validateDate(Date date);

    protected abstract String parseTitle(Document doc);

    protected abstract String parseSource(Document doc);

    protected abstract String parseContent(Document doc);

    protected abstract int getSourceId();

    public void execute(WebDriver driver) {
        List<Article> articles = new ArrayList<>();
        Map<String, Category> urls = this.getUrls();
        for (String url : urls.keySet()) {
            driver.get(url);

            // Scroll to bottom to make sure latest articles are loaded
            PageUtils.scrollToBottom(driver);
            WaitTime.Normal.execute();

            Document doc = Jsoup.parse(driver.getPageSource());
            List<Article> articlesNew = this.parseList(doc);
            articlesNew.forEach(article -> {
                article.setCategory(urls.get(url));
                article.setUrl(Common.getAbsoluteUrl(article.getUrl(), url));
            });
            articles.addAll(articlesNew);
        }

        for (Article article : articles) {
            String logId = Common.toMD5(article.getUrl());
            ActionLog log = dbManager.readById(logId, ActionLog.class);
            if (log != null) {
                continue;
            }
            try {
                this.readArticle(driver, article);
                this.saveArticle(article, driver);
            } catch (PastDateException e) {
                logger.error("Article publish date has past {} minutes: {}", MAX_PAST_MINUTES, article.getUrl());
            } catch (BusinessException e) {
                logger.error("Unable to read article {}", article.getUrl(), e);
            } catch (Exception e) {
                logger.error("Exception found for article: {} -> {}", article.getTitle(), article.getUrl(), e);
                continue;
            }
            dbManager.save(new ActionLog(logId), ActionLog.class);
        }
    }

    void saveArticle(Article article, WebDriver driver) {
        article.checkContent();
        if (article.hasImages()) {
            ImageUploadResult result = this.uploadImages(article, driver);
            if (result != null) {
                List<SavedImage> savedImages = this.saveImages(article, result);
                this.replaceImages(article, savedImages);
            }
        }
        this.cleanThirdPartyImages(article);
        article.checkContent();

        Connection conn = this.createWebConnection(ARTICLE_SAVE_URL, WebDriverLauncher.adminCookies)
            .data("getstring", "")
            .data("mb_no", "")
            .data("ar_status", "1")
            .data("ar_status_old", "")
            .data("ar_copy_edited_finished", "")
            .data("use_article_all_table", "")
            .data("ar_title", article.getTitle())
            .data("ar_mtitle", "")
            .data("ar_summary", article.getSummary())
            .data("ar_content", article.getContent())
            .data("tex", "")
            .data("ar_cat[]", String.valueOf(article.getCategory().id))
            .data("ar_keyword", RandomStringUtils.randomNumeric(10))
            .data("ar_newskeyword", article.getTitle())
            .data("ar_youtube", "")
            .data("ar_typology", "1")
            .data("ar_tag", "")
            .data("ar_topic", "")
            .data("ar_related", "")
            .data("arDate", (new SimpleDateFormat("MM/dd/YYYY HH:mm")).format(article.getDate()))
            .data("ar_reporter", "")
            .data("ar_reporter_email", "")
            .data("ar_originlink", article.getUrl())
            .data("ar_source", this.determineSource(article.getSource(), this.getSourceId()));
        if (article.hasImageIds()) {
            StringBuilder sb = new StringBuilder();

            int length = article.getImageIds().length;
            for (int i = 0; i < length; i++) {
                String imageId = String.valueOf(article.getImageIds()[i]);
                conn.data("imghidden_" + imageId, "")
                    .data("ar_image_hide[" + i + "]", imageId);
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
                    logger.error("Article saved failed: {}", article.getTitle());
                }
            } catch (IOException e) {
                logger.error("Unable to save the article:", e);
                WaitTime.Normal.execute();
            }
        }
        throw new BusinessException(String.format("Unable to save the article: [%s]%s -> %s",
            article.getSource(), article.getTitle(), article.getUrl()));
    }

    void fetchContentImages(Article article, Element contentElm) {
        Elements images = contentElm.select("img");
        List<String> contentImages = new ArrayList<>();
        for (Element image : images) {
            contentImages.add(image.attr("src"));
        }
        article.setContentImages(contentImages);
    }

    Date parseDateText(String timeText, String regex, String dateFormat) {
        try {
            timeText = RegexUtils.getMatched(timeText, regex);
            Date date = DateUtils.parseDate(timeText, Locale.PRC, dateFormat);

            // If dateFormat without year, set as current year
            if (DateFormat.YEAR.format(date).equals(WITHOUT_YEAR)) {
                int year = Calendar.getInstance().get(Calendar.YEAR);
                date = DateUtils.setYears(date, year);
            }
            // If dateFormat without month and day, set today
            if (StringUtils.equals(DateFormat.FULL_MONTH_DAY.format(date), WITHOUT_MONTH_DAY)) {
                int month = Calendar.getInstance().get(Calendar.MONDAY);
                int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
                date = DateUtils.setMonths(date, month);
                date = DateUtils.setDays(date, day);
            }

            if (this.calcMinutesAgo(date) > MAX_PAST_MINUTES) {
                throw new PastDateException();
            }
            return date;
        } catch (ParseException e) {
            throw new BusinessException(String.format("Unable to parse date: %s", timeText));
        }
    }

    Date parseDateTextWithDay(String timeText, String regex, String dateFormat, int maxPastDays) {
        try {
            timeText = RegexUtils.getMatched(timeText, regex);
            Date date = DateUtils.parseDate(timeText, Locale.PRC, dateFormat);

            if (Days.daysBetween(new DateTime(date), DateTime.now()).getDays() > maxPastDays) {
                throw new PastDateException();
            }
            return date;
        } catch (ParseException e) {
            throw new BusinessException(String.format("Unable to parse date: %s", timeText));
        }
    }

    private void cleanThirdPartyImages(Article article) {
        if (!article.hasImages()) {
            return;
        }
        Document dom = Jsoup.parse(article.getContent());
        boolean hasRemoval = false;
        for(Element image : dom.select("img")) {
            if (StringUtils.containsIgnoreCase(image.attr("src"), CDN_URL)) {
                this.justifyImage(image);
                continue;
            }
            image.remove();
            hasRemoval =true;
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
        Connection conn = Jsoup.connect(url)
            .userAgent("Mozilla")
            .method(Method.POST)
            .data("maxFileNum", "50")
            .data("maxFileSize", "20 MB")
            .data("unique_key", Common.toMD5(String.valueOf(System.currentTimeMillis())))
            .data("field", "image_hidden")
            .data("func", "photo_image_content")
            .data("request_from", "ContentCrawler");
        if (cookies != null) {
            conn.cookies(cookies);
        }
        return conn;
    }

    private void replaceImages(Article article, List<SavedImage> savedImages) {
        String content = article.getContent();
        int[] imageIds = new int[savedImages.size()];
        for (String contentImage : article.getContentImages()) {
            for (SavedImage savedImage : savedImages) {
                int index = contentImage.length() - savedImage.getOriginalFile().length();
                if (StringUtils.equalsIgnoreCase(contentImage.substring(index), savedImage.getOriginalFile())) {
                    String newImage = CDN_URL + DATA_IMAGES_FULL + savedImage.getPath();
                    content = StringUtils.replace(content, contentImage, newImage);
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
    ImageUploadResult uploadImages(Article article, WebDriver driver) {
        if (!article.hasImages()) {
            return null;
        }
        Connection conn = this.createWebConnection(FILE_UPLOAD_URL, null);

        int i = 0;
        for (String imageUrl : article.getContentImages()) {
            imageUrl = Common.getAbsoluteUrl(imageUrl, article.getUrl());
            String image = this.downloadFile(imageUrl, driver);
            File file = FileUtils.getFile(image);
            try {
                FileInputStream fs = new FileInputStream(file);
                conn.data("files[" + i + "]", image, fs);
                i++;
            } catch (IOException e) {
                logger.error("Unable to download file: {}", image, e);
            }
        }
        for (int j = 0; j < Constants.MAX_REPEAT_TIMES; j++) {
            try {
                String body = conn.execute().body();
                ImageUploadResult result = JSONObject.parseObject(body, ImageUploadResult.class);
                if (!result.hasFiles()) {
                    logger.error("Files are not uploaded, retry uploading: {}", body);
                    continue;
                }
                return result;
            } catch (IOException e) {
                logger.error("Unable to upload files, retry in {} seconds:", WaitTime.Normal.val(),  e);
                WaitTime.Normal.execute();
            }
        }
        return null;
    }

    /**
     * Save uploaded images to DB, and move out of the temp directory
     */
    private List<SavedImage> saveImages(Article article, ImageUploadResult result) {
        Connection conn = this.createWebConnection(FILE_SAVE_URL, WebDriverLauncher.adminCookies);

        int i = 0;
        for (UploadedImage imageFile : result.getFiles()) {
            conn.data("im_title[" + i + "]", article.getTitle())
                .data("im_content[" + i + "]", "")
                .data("im_credit[" + i + "]", article.getSource())
                .data("im_link[" + i + "]", article.getUrl())
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
                logger.error("Unable to save the files, retry in {} seconds:", WaitTime.Normal.val(),  e);
                WaitTime.Normal.execute();
            }
        }
        return null;
    }

    /**
     * Download image from its url
     */
    private String downloadFile(String url, WebDriver driver) {
        String fileName = Common.extractFileNameFromUrl(url);
        File file = new File(DOWNLOAD_PATH, fileName);
        if (!file.canWrite()) {
            file.setWritable(true);
        }

        String prefix = Tools.startWithAny(url, Constants.HTTP) ? StringUtils.EMPTY : Constants.HTTP + ":";
        HttpGet get = HttpUtils.prepareHttpGet(prefix + url);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        BasicHttpContext localContext = new BasicHttpContext();
        localContext.setAttribute(HttpClientContext.COOKIE_STORE, PageUtils.getCookieStore(driver));

        long start = System.currentTimeMillis();
        CloseableHttpResponse resp = null;
        InputStream is = null;
        try {
            resp = httpClient.execute(get, localContext);
            int status = resp.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_OK) {
                is = resp.getEntity().getContent();
                FileUtils.copyInputStreamToFile(is, file);
                String path = file.getAbsolutePath();
                logger.info("{} {} file downloaded, time costs {}. Size:{}, path:{}", fileName, Tools.formatCostTime(start), FileUtils.byteCountToDisplaySize(file.length()), path);
                return path;
            }
            throw new BusinessException(String.format("Failed to execute %s file download request. Status code: %s", fileName, status));
        } catch (IOException ex) {
            throw new BusinessException(String.format("Failed to download file of %s： %s", fileName, Tools.getExceptionMsg(ex)));
        } finally {
            get.releaseConnection();
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(resp);
        }
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

    String cleanHtml(Element dom) {
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
                    // Replace a tag name to span
                    element.tagName("span");
                }
            }
            this.removeNeedlessHtmlTags(element);
        }
    }

    private boolean isAllowedTag(String tagName) {
        return StringUtils.equalsIgnoreCase(tagName, "table") ||
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
        return Tools.containsAny(tagName, "div", "h2", "h3", "h4");
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
