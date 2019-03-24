package cn.btimes.model.common;

import cn.btimes.ui.ContentCrawler.Application;
import lombok.Data;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-03-11 9:42 AM
 */
@Data
public class Config {
    private String adminUrl;
    private String adminEmail;
    private String adminPassword;
    private String cdnUrl;
    private String fileUploadUrl;
    private String fileSaveUrl;
    private String articleSaveUrl;
    private String dataImagesFull;
    private int maxPastMinutes = 180;
    private Application application;
}
