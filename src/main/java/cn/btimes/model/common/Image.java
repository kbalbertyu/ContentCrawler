package cn.btimes.model.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/7/30 11:35
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Image {
    private String url;
    private String content;
}
