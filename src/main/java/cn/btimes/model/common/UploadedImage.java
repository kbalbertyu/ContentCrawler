package cn.btimes.model.common;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 12/26/2018 10:18 PM
 */
@Data
public class UploadedImage {
    private String originalFile;
    private String name;
    private String size;
    private String error;
    private String content = StringUtils.EMPTY;
    private String imLink;
}
