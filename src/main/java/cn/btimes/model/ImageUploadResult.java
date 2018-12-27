package cn.btimes.model;

import lombok.Data;

import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 12/26/2018 10:16 PM
 */
@Data
public class ImageUploadResult {
    private List<UploadedImage> files;
}
