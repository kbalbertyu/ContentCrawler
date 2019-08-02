package cn.btimes.service;

import cn.btimes.model.common.Config;
import cn.btimes.model.shenma.ShenmaConfig;
import cn.btimes.model.shenma.ShenmaResult;
import com.alibaba.fastjson.JSONObject;
import com.amzass.enums.common.Directory;
import com.amzass.model.common.ActionLog;
import com.amzass.utils.common.Tools;
import com.google.inject.Inject;
import com.kber.commons.DBManager;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/8/1 17:38
 */
public class ShenmaLinksUploader extends AbstractLinksUploader {
    private final Logger logger = LoggerFactory.getLogger(ShenmaLinksUploader.class);
    @Inject private DBManager dbManager;
    private static final String URL_ID_PREFIX = "Shenma";
    private static final String POST_URL_FORMAT = "http://data.zhanzhang.sm.cn/push?site=%s&user_name=%s&resource_name=%s&token=%s";

    @Override
    public void execute(Config config) {
        ShenmaConfig smConfig = this.loadConfig();
        logger.info("Uploading Shenma MIP links");
        String fetchUrl = String.format("/article/fetchBaiduSiteMap?dev=mip&only_original=1&article_days=%d",
            config.getBaiduDaysBefore());
        List<String> urls = this.fetchLinks(fetchUrl, config, URL_ID_PREFIX);
        if (urls == null || urls.size() == 0) {
            logger.warn("No site map urls found");
            return;
        }
        this.uploadSiteMap(urls, smConfig);
    }

    private ShenmaConfig loadConfig() {
        String configStr = Tools.readFileToString(FileUtils.getFile(Directory.Customize.path(), "shenma.json"));
        return JSONObject.parseObject(configStr, ShenmaConfig.class);
    }

    private void uploadSiteMap(List<String> urls, ShenmaConfig smConfig) {
        String postUrl = String.format(POST_URL_FORMAT, smConfig.getSite(), smConfig.getUsername(),
            smConfig.getResourceName(), smConfig.getToken());
        int itemsPerUpload = 1000;
        int size = urls.size();
        for (int i = 0; ; i++) {
            List<String> urlsForUpload = this.urlsSegment(urls, size, i, itemsPerUpload);
            if (CollectionUtils.isEmpty(urlsForUpload)) {
                logger.warn("No urls found for Shenma submission");
                break;
            }
            String data = StringUtils.join(urlsForUpload, StringUtils.LF);

            String body = this.postLinks(postUrl, data, ShenmaConfig.API_HOST);
            ShenmaResult result = JSONObject.parseObject(body, ShenmaResult.class);
            if (!result.success()) {
                logger.error("SiteMap urls not imported: {}", result.getErrorMsg());
                return;
            }
            logger.info("SiteMap urls imported: {}", result);
            urlsForUpload.forEach(url -> dbManager.save(new ActionLog(urlId(url, URL_ID_PREFIX)), ActionLog.class));
        }
    }
}
