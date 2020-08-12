package cn.btimes.model.common;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/8/12 6:37
 */
@Data
public class HaiWaiNetResult {

    @JSONField(name = "error_code")
    private int errorCode;

    @JSONField(name = "result")
    HaiWaiNetArticle article;

    public boolean valid() {
        return errorCode == 0;
    }
}
