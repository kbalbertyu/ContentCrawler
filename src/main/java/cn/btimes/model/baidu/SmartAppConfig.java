package cn.btimes.model.baidu;

import cn.btimes.model.common.Category;
import lombok.Data;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/6/10 9:23
 */
@Data
public class SmartAppConfig {
    private SmartApp app;
    private String username;
    private String password;
    private Category category;
}
