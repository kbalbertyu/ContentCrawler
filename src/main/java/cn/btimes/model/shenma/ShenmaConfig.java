package cn.btimes.model.shenma;

import lombok.Data;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/8/2 8:27
 */
@Data
public class ShenmaConfig {
    public static final String API_HOST = "data.zhanzhang.sm.cn";

    private String site;
    private String username;
    private String resourceName;
    private String token;
}
