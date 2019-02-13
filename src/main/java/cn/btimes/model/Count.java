package cn.btimes.model;

import cn.btimes.utils.Common;
import lombok.Data;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-02-12 10:16 PM
 */
@Data
public class Count {
    private int total;
    private int published;

    public String publishedText() {
        return published + "(" + this.percent() + ")";
    }

    private String percent() {
        if (total == 0 || published == 0) {
            return "0";
        }
        return Common.percentage(published * 100.0f / total);
    }

    void increase(boolean published) {
        total++;
        if (published) {
            this.published++;
        }
    }
}
