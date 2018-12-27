package cn.btimes.model;

import lombok.Data;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 12/26/2018 10:18 PM
 */
@Data
public class UploadedImage {
    private String originalFile;
    private String name;
    private String size;
    private String error;
}
