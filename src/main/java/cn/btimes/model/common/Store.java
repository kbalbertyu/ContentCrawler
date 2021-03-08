package cn.btimes.model.common;

import lombok.Data;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/11/21 22:58
 */
@Data
public class Store {
    private String name;
    private List<String> gallery;
    private List<String> videos;
    private List<String> certificates;

    public boolean valid() {
        return CollectionUtils.isNotEmpty(gallery) || CollectionUtils.isNotEmpty(videos);
    }
}
