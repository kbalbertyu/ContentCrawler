package cn.kpopstarz.source;

import cn.btimes.model.common.Article;
import cn.btimes.model.common.BTExceptions.PastDateException;
import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;
import com.amzass.utils.common.Constants;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/6/12 17:21
 */
public class People extends cn.btimes.source.People {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://korea.people.com.cn/GB/407885/", Category.General);
        URLS.put("http://korea.people.com.cn/GB/407885/407921/index.html", Category.General);
        URLS.put("http://korea.people.com.cn/GB/407863/411398/index.html", Category.General);
        URLS.put("http://korea.people.com.cn/GB/407891/407918/index.html", Category.General);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".headingNews > .hdNews", "#rwb_zw", "strong > a", "em > a",
            "", ".box01 > .fl");
    }

    @Override
    protected int getSourceId() {
        return 0;
    }

    @Override
    protected List<Article> parseList(Document doc) {
        List<Article> articles = new ArrayList<>();
        Elements list = this.readList(doc);
        String today = DateFormatUtils.format(new Date(), "yyyy/MMdd");
        int i = 0;
        for (Element row : list) {
            try {
                Article article = new Article();
                this.parseTitle(row, article);
                if (!StringUtils.contains(article.getUrl(), today)) {
                    throw new PastDateException();
                }

                articles.add(article);
            } catch (PastDateException e) {
                if (i++ < Constants.MAX_REPEAT_TIMES) {
                    continue;
                }
                logger.warn("Article that past 1 day detected, complete the list fetching: ", e);
                break;
            }
        }
        return articles;
    }
}
