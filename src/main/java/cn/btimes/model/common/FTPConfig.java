package cn.btimes.model.common;

import lombok.Data;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/10/13 21:01
 */
@Data
public class FTPConfig {
    private String host;
    private String username;
    private String pwd;
    private int port = 21;
    private boolean activeMode = true;
}
