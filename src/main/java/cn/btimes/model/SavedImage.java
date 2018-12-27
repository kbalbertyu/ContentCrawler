package cn.btimes.model;

import lombok.Data;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 12/27/2018 8:34 AM
 */
@Data
public class SavedImage {
    private String originalFile;
    private String uploadedFile;
    private int imageId;
    private String fileName;
    private String path;
}
