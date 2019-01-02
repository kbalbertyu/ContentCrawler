package cn.btimes.source;

import cn.btimes.model.Article;
import cn.btimes.model.BTExceptions.PastDateException;
import cn.btimes.model.Category;
import com.amzass.service.sellerhunt.HtmlParser;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.amzass.utils.common.RegexUtils;
import com.amzass.utils.common.Tools;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2018-12-31 10:15 AM
 */
public class Sina extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String DATE_REGEX = "\\d{1,2}:\\d{1,2}";
    private static final String DATE_FORMAT = "HH:mm";
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://finance.sina.com.cn/chanjing/", Category.COMPANY);
        URLS.put("http://finance.sina.com.cn/china/", Category.ECONOMY);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = doc.select(".feed-card-item");
        for (Element row : list) {
            try {
                Article article = new Article();

                if (StringUtils.isBlank(row.html())) {
                    continue;
                }

                String timeText = HtmlParser.text(row, ".feed-card-time");
                article.setDate((this.parseDateText(timeText)));

                Element linkElm = row.select("h2 > a").get(0);
                article.setUrl(linkElm.attr("href"));
                article.setTitle(linkElm.text());

                article.setSummary(HtmlParser.text(row, ".feed-card-txt-summary"));

                articles.add(article);
            } catch (PastDateException e) {
                logger.warn("Article that past {} minutes detected, complete the list fetching.", MAX_PAST_MINUTES);
                break;
            }
        }
        return articles;
    }

    @Override
    protected Boolean validateLink(String href) {
        return null;
    }

    @Override
    protected void readArticle(WebDriver driver, Article article) {
        driver.get(article.getUrl());
        WaitTime.Normal.execute();
        Document doc = Jsoup.parse(driver.getPageSource());
        Element contentElm = doc.select("#artibody").first();
        article.setContent(this.cleanHtml(contentElm));
        this.fetchContentImages(article, contentElm);
    }

    @Override
    protected Date parseDateText(String timeText) {
        if (Tools.containsAny(timeText, "刚刚")) {
            return new Date();
        }
        if (!Tools.containsAny(timeText, "分钟", "今天")) {
            throw new PastDateException();
        }

        int minutes;
        if (Tools.contains(timeText, "分钟")) {
            minutes = NumberUtils.toInt(RegexUtils.getMatched(timeText, "\\d+"));
            if (minutes == 0) {
                throw new BusinessException("Unable to parse time text: " + timeText);
            }
        } else {
            Date date = this.parseDateText(timeText, DATE_REGEX, DATE_FORMAT);
            minutes = this.calcMinutesAgo(date);
        }

        if (minutes > MAX_PAST_MINUTES) {
            throw new PastDateException();
        }
        return DateUtils.addMinutes(new Date(), -1 * minutes);
    }

    @Override
    protected Date parseDate(Document doc) {
        return null;
    }

    @Override
    protected void validateDate(Date date) {

    }

    @Override
    protected String parseTitle(Document doc) {
        return null;
    }

    @Override
    protected String parseSource(Document doc) {
        return StringUtils.EMPTY;
    }

    @Override
    protected String parseContent(Document doc) {
        return null;
    }

    @Override
    protected int getSourceId() {
        return 101;
    }

    /**
     * 下一步可能要做的有：
     * 删除“本报讯”段落或关键词
     */
    @Override
    String cleanHtml(Element dom) {
        dom.select("[id^=ad], [class^=survey], [id^=quote_], script, .article-editor, p:contains(本文来自于), p:contains(原题为), p:contains(责任编辑), span[style*=KaiTi_GB2312], p:contains(来源：), p:contains(免责声明：)").remove();
        return super.cleanHtml(dom);
    }
}
