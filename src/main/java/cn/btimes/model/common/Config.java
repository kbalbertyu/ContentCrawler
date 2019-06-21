package cn.btimes.model.common;

import cn.btimes.ui.ContentCrawler.Application;
import lombok.Data;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-03-11 9:42 AM
 */
@Data
public class Config {
    private String frontUrl;
    private String adminUrl;
    private String adminEmail;
    private String recipient = "maintain@btimes.com.cn";
    private String developerEmail = "tansoyu@gmail.com";
    private String adminPassword;
    private String cdnUrl;
    private String fileUploadUrl;
    private String fileSaveUrl;
    private String articleSaveUrl;
    private String dataImagesFull;
    private int maxPastMinutes = 180;
    private Application application;
    private int maxPastHours;
    private String baiduAMPSite;
    private String baiduAMPToken;
    private int baiduAMPDaysBefore = 3;

    public void init() {
        this.maxPastHours = this.maxPastMinutes / 60;
    }
}
