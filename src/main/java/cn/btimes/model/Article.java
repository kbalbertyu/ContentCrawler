package cn.btimes.model;

import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 12/24/2018 10:57 PM
 */
@Data
public class Article {
    private String url;
    private String title;
    private Date date;
    private String summary = StringUtils.EMPTY;
    private String content;
    private String reporter = StringUtils.EMPTY;
    private String source;
    private List<String> contentImages;
    private int[] imageIds;

    public boolean hasImages() {
        return CollectionUtils.isNotEmpty(contentImages);
    }

    public boolean hasImageIds() {
        return ArrayUtils.isNotEmpty(imageIds);
    }
}
