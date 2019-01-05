package cn.btimes.source;

import cn.btimes.model.Article;
import cn.btimes.model.BTExceptions.PastDateException;
import cn.btimes.model.Category;
import com.amzass.service.sellerhunt.HtmlParser;
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
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 12/25/2018 1:07 AM
 */
public class ThePaper extends Source {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://www.thepaper.cn/list_25434", Category.COMPANY);
        URLS.put("https://www.thepaper.cn/list_27234", Category.TECH);
        URLS.put("https://www.thepaper.cn/list_25433", Category.REALESTATE);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        int i = 0;
        List<Article> articles = new ArrayList<>();
        Elements list = doc.select(".newsbox .news_li");
        for (Element row : list) {
            try {
                Article article = new Article();
                if (StringUtils.isBlank(row.html())) {
                    continue;
                }

                String timeText = HtmlParser.text(row, ".pdtt_trbs > span");
                try {
                    article.setDate(this.parseDateText(timeText));
                } catch (PastDateException e) {
                    if (i++ < Constants.MAX_REPEAT_TIMES) {
                        continue;
                    }
                    throw e;
                }

                Element linkElm = row.select("h2 > a").get(0);
                article.setUrl(linkElm.attr("href"));
                article.setTitle(linkElm.text());

                article.setSummary(HtmlParser.text(row, "p"));

                articles.add(article);
            } catch (PastDateException e) {
                logger.warn("Article that past {} minutes detected, complete the list fetching.", MAX_PAST_MINUTES);
                break;
            }
        }
        return articles;
    }

    protected Date parseDateText(String timeText) {
        if (Tools.containsAny(timeText, "刚刚")) {
            return new Date();
        }
        if (!Tools.containsAny(timeText, "分钟")) {
            throw new PastDateException();
        }
        int minutes = NumberUtils.toInt(RegexUtils.getMatched(timeText, "\\d+"));
        if (minutes == 0) {
            throw new BusinessException("Unable to parse time text: " + timeText);
        }
        if (minutes > MAX_PAST_MINUTES) {
            throw new PastDateException();
        }
        return DateUtils.addMinutes(new Date(), -1 * minutes);
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

        article.setTitle(this.parseTitle(doc));
        article.setSource(this.parseSource(doc));

        Element contentElm = doc.select(".news_txt").first();
        article.setContent(this.cleanHtml(contentElm));
        this.fetchContentImages(article, contentElm);
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
        return HtmlParser.text(doc, "h1.news_title");
    }

    @Override
    protected String parseSource(Document doc) {
        String sourceText = HtmlParser.text(doc, ".news_about > p > span:contains(来源)");
        return StringUtils.trim(StringUtils.remove(sourceText, "来源："));
    }

    @Override
    protected String parseContent(Document doc) {
        // Not used here
        return null;
    }

    @Override
    protected int getSourceId() {
        return 12;
    }
}
