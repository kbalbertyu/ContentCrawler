package cn.btimes.model;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-21 11:19 PM
 */
@Data
public class Messenger {
    private String source;
    private String message;

    public Messenger(String source, String message) {
        if (StringUtils.contains(source, ".")) {
            String[] parts = StringUtils.split(source, ".");
            source = parts[parts.length - 1];
        }
        this.source = source;
        this.message = message;
    }
}
