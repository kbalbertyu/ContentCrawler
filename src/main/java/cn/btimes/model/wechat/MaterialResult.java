package cn.btimes.model.wechat;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/1/8 15:52
 */
@Data
public class MaterialResult {
    @JSONField(name = "total_count")
    private int totalCount;
    @JSONField(name = "item_count")
    private int itemCount;
    @JSONField(name = "item")
    private List<Item> items;
}
