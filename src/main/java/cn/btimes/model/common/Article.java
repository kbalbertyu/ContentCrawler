package cn.btimes.model.common;

import com.amzass.utils.common.Exceptions.BusinessException;
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
    private String source = StringUtils.EMPTY;
    private List<String> contentImages;
    private int[] imageIds;
    private Category category;

    public boolean hasImages() {
        return CollectionUtils.isNotEmpty(contentImages);
    }

    public boolean hasImageIds() {
        return ArrayUtils.isNotEmpty(imageIds);
    }

    public void checkContent() {
        if (StringUtils.isBlank(content)) {
            throw new BusinessException(String.format("Article content is blank: %s -> %s", title, url));
        }
    }
}
