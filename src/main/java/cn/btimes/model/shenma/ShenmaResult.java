package cn.btimes.model.shenma;

import lombok.Data;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/8/2 9:01
 */
@Data
public class ShenmaResult {
    private int returnCode;
    private String errorMsg;

    public boolean success() {
        return returnCode == 200;
    }
}