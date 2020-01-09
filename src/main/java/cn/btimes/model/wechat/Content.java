package cn.btimes.model.wechat;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/1/8 15:57
 */
@Data
public class Content {
    @JSONField(name = "news_item")
    private List<News> newsList;
    @JSONField(name = "create_time")
    private long createTime;
    @JSONField(name = "update_time")
    private long updateTime;
}
