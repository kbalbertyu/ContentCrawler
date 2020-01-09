package cn.btimes.model.wechat;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/1/8 15:54
 */
@Data
public class Item {
    @JSONField(name = "media_id")
    private String mediaId;
    @JSONField(name = "update_time")
    private long updateTime;
    private Content content;
}
