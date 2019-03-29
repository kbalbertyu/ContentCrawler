package cn.btimes.service;

import cn.btimes.model.common.ArticleData;
import cn.btimes.model.common.ArticleSource;
import cn.btimes.model.common.Category;
import cn.btimes.model.common.Config;
import cn.btimes.model.stat.Count;
import cn.btimes.model.stat.Stat;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.mailman.model.common.WebApiResult;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-31 4:52 PM
 */
public class Statistics {
    private static final String TABLE_BORDER = "style=\"border:1px solid #ccc;\"";
    private static final String TD_LEFT = "<td " + TABLE_BORDER + " >";
    private static final String TH_LEFT = "<th " + TABLE_BORDER + " >";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Inject private ApiRequest apiRequest;
    @Inject private EmailSenderHelper emailSenderHelper;

    public void execute(Config config) {
        WebApiResult result = apiRequest.get("/article/crawledArticleStat", config);
        if (result != null) {
            String data = result.getData();
            try {
                List<ArticleData> articleDataList = JSONObject.parseArray(data, ArticleData.class);
                if (CollectionUtils.isEmpty(articleDataList)) {
                    logger.warn("No stat data found: {}", data);
                    return;
                }
                this.stat(articleDataList, config);
            } catch (JSONException e) {
                logger.error("Unable to parse stat result: {}", data, e);
            }
        }
    }

    private void stat(List<ArticleData> articleDataList, Config config) {
        Map<ArticleSource, Stat> stats = new TreeMap<>();
        for (ArticleData articleData : articleDataList) {
            ArticleSource source = articleData.source();
            Category category = articleData.category();
            Stat stat = stats.getOrDefault(source, new Stat());
            stat.increase(articleData.published(), category);
            stats.put(source, stat);
        }
        List<Entry<ArticleSource, Stat>> sortedStats = this.sortStat(stats);

        StringBuilder sb = new StringBuilder();
        this.statsToHtml(sortedStats, sb);
        String subject = "近7天内文章抓取统计";
        this.sendMessage(subject, sb.toString(), config);
    }

    private List<Entry<ArticleSource, Stat>> sortStat(Map<ArticleSource, Stat> stats) {
        List<Entry<ArticleSource, Stat>> mapList = new ArrayList<>(stats.entrySet());
        Comparator<Entry<ArticleSource, Stat>> comparator = Comparator.comparing(o -> o.getValue());
        mapList.sort(comparator.reversed());
        return mapList;
    }

    private void statsToHtml(List<Entry<ArticleSource, Stat>> stats, StringBuilder sb) {
        sb.append("<table " + TABLE_BORDER + " cellspacing=\"0\">");
        sb.append("<tr>");
        sb.append(TH_LEFT + "来源</th>");
        sb.append(TH_LEFT + "栏目</th>");
        sb.append(TH_LEFT + "已发布</th>");
        sb.append(TH_LEFT + "总数</th>");
        sb.append("</tr>");

        for (Entry<ArticleSource, Stat> stat : stats) {
            this.statToHtml(stat, sb);
        }
        sb.append("</table>");
    }

    private void statToHtml(Entry<ArticleSource, Stat> stat, StringBuilder sb) {
        ArticleSource source = stat.getKey();
        Map<Category, Count> categoryStats = stat.getValue().getCategoryStats();
        Count count = stat.getValue().getCount();
        int size = categoryStats.size();

        sb.append("<tr>");
        sb.append("<th rowspan=\"" + (size + 1) + "\" " + TABLE_BORDER + " >");
        sb.append(source.getSourceKey());
        sb.append("</th>");
        sb.append(TH_LEFT);
        sb.append("全部");
        sb.append("</th>");
        sb.append(TH_LEFT);
        sb.append(count.publishedText());
        sb.append("</th>");
        sb.append(TH_LEFT);
        sb.append(count.getTotal());
        sb.append("</th>");
        sb.append("</tr>");

        for (Category category : categoryStats.keySet()) {
            Count categoryCount = categoryStats.get(category);
            sb.append("<tr>");
            sb.append(TD_LEFT);
            sb.append(category.title);
            sb.append("</td>");
            sb.append(TD_LEFT);
            sb.append(categoryCount.publishedText());
            sb.append("</td>");
            sb.append(TD_LEFT);
            sb.append(categoryCount.getTotal());
            sb.append("</td>");
            sb.append("</tr>");
        }
    }

    private void sendMessage(String subject, String content, Config config) {
        this.emailSenderHelper.send(subject, content, config.getDeveloperEmail(), config.getRecipient());
    }
}
