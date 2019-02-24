package cn.btimes.model.common;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-10 8:21 PM
 */
@Data
@AllArgsConstructor
public class DownloadResult {
    private String url;
    private String fullPath;
    private String fileName;
}
