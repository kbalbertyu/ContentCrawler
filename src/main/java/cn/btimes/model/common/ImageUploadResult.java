package cn.btimes.model.common;

import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 12/26/2018 10:16 PM
 */
@Data
public class ImageUploadResult {
    private List<UploadedImage> files;

    private void filterErrorFiles() {
        files.removeIf(file -> StringUtils.isNotBlank(file.getError()));
    }

    public boolean hasFiles() {
        this.filterErrorFiles();
        return !CollectionUtils.isEmpty(files);
    }
}
