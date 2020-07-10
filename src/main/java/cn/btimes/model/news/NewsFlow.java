package cn.btimes.model.news;

import lombok.Data;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/6/29 12:02
 */
@Data
public class NewsFlow {
    private String title;
    private String date;
    private String source;
    private boolean important;
    private boolean bold;
}
