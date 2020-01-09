package cn.btimes.model.wechat;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/1/8 15:58
 */
@Data
public class News {
    private String title;
    @JSONField(name = "thumb_media_id")
    private String thumbMediaId;
    @JSONField(name = "show_cover_pic")
    private boolean showCoverPic;
    private String author;
    private String digest;
    private String content;
    private String url;
    @JSONField(name = "content_source_url")
    private String contentSourceUrl;
}
