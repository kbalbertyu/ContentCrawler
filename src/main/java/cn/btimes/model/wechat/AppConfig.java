package cn.btimes.model.wechat;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/1/8 14:42
 */
@Data
public class AppConfig {
    private String appId;
    private String appSecret;
    private String token;
    private long tokenExpiry;

    public void updateToken(Token token) {
        this.token = token.getToken();
        this.tokenExpiry = System.currentTimeMillis() / 1000 + token.getExpiry();
    }

    public boolean tokenExpired() {
        return StringUtils.isBlank(token) ||
            System.currentTimeMillis() / 1000 > (tokenExpiry - 10);
    }
}
