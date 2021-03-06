package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.BTExceptions.PastDateException;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import com.alibaba.fastjson.JSONObject;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.common.Constants;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.amzass.utils.common.RegexUtils;
import com.amzass.utils.common.Tools;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-05 8:03 PM
 */
public class IYiOu extends SourceWithoutDriver {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.iyiou.com/smartcity/", Category.FUTURE_INDUSTRIES);
        URLS.put("https://www.iyiou.com/new_manufacturing/", Category.FUTURE_INDUSTRIES);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected String getDateRegex() {
        return "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}";
    }

    @Override
    protected String getDateFormat() {
        return "yyyy-MM-dd'T'HH:mm:ss";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery("ul.newestArticleList > li.thinkTankTag", "",
            "h2", "#post_brief", "", "");
    }

    @Override
    public void parseTitle(Element doc, Article article) {
        super.parseTitle(doc, article);
        Elements linkElms = doc.select(this.getCSSQuery().getTitle());
        if (linkElms.size() > 0) {
            article.setUrl(linkElms.get(0).parent().attr("href"));
        }
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        String today = DateFormatUtils.format(new Date(), "yyyy-MM-dd");
        int i = 0;
        for (Element row : list) {
            try {
                Article article = new Article();
                String timeText = HtmlParser.text(row, ".time");
                if ((StringUtils.contains(timeText, "小时前") && !this.checkHoursBefore(timeText)) ||
                    (RegexUtils.match(timeText, "\\d{4}-\\d{2}-\\d{2}") && !StringUtils.contains(timeText, today))) {
                    throw new PastDateException("Time past limit: " + timeText);
                }

                this.parseTitle(row, article);
                articles.add(article);
            } catch (PastDateException e) {
                if (i++ < Constants.MAX_REPEAT_TIMES) {
                    continue;
                }
                logger.warn("Article that past {} minutes detected, complete the list fetching: ", config.getMaxPastMinutes(), e);
                break;
            }
        }
        return articles;
    }

    @Override
    protected String cleanHtml(Element dom) {
        Elements elements = dom.select("#machineContainer, .copyrightState, p:contains(本文来源)");
        if (elements.size() > 0) {
            elements.remove();
        }
        return super.cleanHtml(dom);
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        this.readDateSummaryContent(driver, article);
    }

    private void convertImageToNoneHttps(Element contentElm) {
        Elements images = contentElm.select("img");
        for (Element image : images) {
            String src = image.attr("src");
            if (StringUtils.startsWith(src, Constants.HTTPS)) {
                src = StringUtils.replace(src, Constants.HTTPS, Constants.HTTP);
                image.attr("src", src);
            }
        }
    }

    @Override
    protected Date parseDateText(String timeText) {
        if (Tools.containsAny(timeText, "刚刚")) {
            return new Date();
        }
        if (!Tools.containsAny(timeText, "分钟前", "小时前")) {
            return super.parseDateText(timeText);
        }
        int minutes = NumberUtils.toInt(RegexUtils.getMatched(timeText, "\\d+"));
        if (Tools.containsAny(timeText, "小时前")) {
            minutes = extractFromHoursBefore(timeText) * 60;
        } else if (!Tools.containsAny(timeText, "分钟")) {
            if (minutes == 0) {
                throw new BusinessException("Unable to parse time text: " + timeText);
            }
        }

        if (minutes > config.getMaxPastMinutes()) {
            throw new PastDateException("Time past limit: " + timeText);
        }
        return DateUtils.addMinutes(new Date(), -1 * minutes);
    }

    @Override
    protected void parseDate(Element doc, Article article) {
        String dateTextCssQuery = "script[type=application/ld+json]";
        this.checkDateTextExistence(doc, dateTextCssQuery);
        Elements elements = doc.select(dateTextCssQuery);
        if (elements.size() > 0) {
            JSONObject obj = JSONObject.parseObject(elements.get(0).html());
            article.setDate(this.parseDateText(String.valueOf(obj.get("pubDate"))));
        } else {
            dateTextCssQuery = "#post_date";
            this.checkDateTextExistence(doc, dateTextCssQuery);
            String dateText = HtmlParser.text(doc, dateTextCssQuery);
            article.setDate(this.parseDateText(dateText));
        }
    }

    @Override
    protected void parseSummary(Element doc, Article article) {
        String summaryCssQuery = this.getCSSQuery().getSummary();
        doc.select(summaryCssQuery).get(0).select("b").remove();
        super.parseSummary(doc, article);
    }

    @Override
    protected void parseContent(Document doc, Article article) {
        String cssQuery = "#post_description";
        this.checkArticleContentExistence(doc, cssQuery);
        Element contentElm = doc.select(cssQuery).first();
        Elements thumbnailELms = doc.select("#post_thumbnail");
        if (thumbnailELms.size() > 0) {
            Element thumbElm = thumbnailELms.first();
            thumbElm.select(".post_copyright").remove();
            contentElm.prepend(thumbElm.outerHtml());
        }

        this.convertImageToNoneHttps(contentElm);
        article.setContent(this.cleanHtml(contentElm));
        this.fetchContentImages(article);
    }

    @Override
    protected int getSourceId() {
        return 1004;
    }
}
