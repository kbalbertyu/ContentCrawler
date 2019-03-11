package cn.btimes.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.BTExceptions.PastDateException;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import com.amzass.utils.PageLoadHelper.WaitTime;
import com.amzass.utils.common.Constants;
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
    protected String getDateRegex() {
        return "\\d{1,2}:\\d{1,2}";
    }

    @Override
    protected String getDateFormat() {
        return "HH:mm";
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".feed-card-item", "#artibody", "h2 > a",
            ".feed-card-txt-summary", ".date-source > a.source", ".feed-card-time");
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        int i = 0;
        for (Element row : list) {
            try {
                Article article = new Article();
                if (StringUtils.isBlank(row.html())) {
                    continue;
                }

                this.parseDate(row, article);
                this.parseTitle(row, article);
                this.parseSummary(row, article);

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
    protected void readArticle(WebDriver driver, Article article) {
        driver.get(article.getUrl());
        WaitTime.Normal.execute();
        Document doc = Jsoup.parse(driver.getPageSource());
        this.parseContent(doc, article);
    }

    @Override
    protected Date parseDateText(String timeText) {
        if (Tools.containsAny(timeText, "刚刚")) {
            return new Date();
        }
        if (!Tools.containsAny(timeText, "分钟", "今天")) {
            throw new PastDateException("Time not contains minutes or today: " + timeText);
        }

        if (Tools.contains(timeText, "分钟")) {
            int minutes = NumberUtils.toInt(RegexUtils.getMatched(timeText, "\\d+"));
            if (minutes == 0) {
                throw new BusinessException("Unable to parse time text: " + timeText);
            }

            if (minutes > config.getMaxPastMinutes()) {
                throw new PastDateException("Time past limit: " + timeText);
            }
            return DateUtils.addMinutes(new Date(), -1 * minutes);
        } else {
            return super.parseDateText(timeText);
        }
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
    protected String cleanHtml(Element dom) {
        Elements elements = dom.select("[id^=ad], [class^=survey], [id^=quote_], script, .article-editor, p:contains(本文来自于), p:contains(原题为), p:contains(责任编辑), span[style*=KaiTi_GB2312], p:contains(来源：), p:contains(免责声明：)");
        if (elements.size() > 0) {
            elements.remove();
        }
        return super.cleanHtml(dom);
    }
}
