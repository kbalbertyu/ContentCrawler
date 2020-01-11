package cn.btimes.model.wechat;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

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

    public boolean invalid() {
        return errcode == 0 || StringUtils.isNotBlank(errmsg);
    }
}
