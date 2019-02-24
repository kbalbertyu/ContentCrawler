package cn.btimes.model.common;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-23 4:35 PM
 */
@Data
@AllArgsConstructor
public class CSSQuery {
    private String list;
    private String content;
    private String title;
    private String summary;
    private String source;
    private String time;
}
