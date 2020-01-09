package cn.btimes.model.wechat;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/1/8 14:57
 */
@Data
public class Token {
    @JSONField(name = "access_token")
    private String token;
    @JSONField(name = "expires_in")
    private long expiry;
    private int errcode;
    private String errmsg;
}
