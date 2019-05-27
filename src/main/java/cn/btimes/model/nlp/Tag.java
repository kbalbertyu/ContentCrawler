package cn.btimes.model.nlp;

import lombok.Data;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-05-24 8:57 AM
 */
@Data
public class Tag {
    private int id;
    private Double score;
    private String tag;
}
