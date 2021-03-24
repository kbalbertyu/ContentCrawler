package cn.btimes.model.common;

import com.alibaba.fastjson.annotation.JSONField;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.amzass.utils.common.Tools;
import com.google.common.base.Objects;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 12/24/2018 10:57 PM
 */
@Data
public class Article {
    @JSONField(name = "ar_id")
    private int id;
    private String url = StringUtils.EMPTY;
    @JSONField(name = "ar_title")
    private String title;
    private Date date;
    private String summary = StringUtils.EMPTY;
    @JSONField(name = "ar_content")
    private String content = StringUtils.EMPTY;
    private String reporter = StringUtils.EMPTY;
    private String source = StringUtils.EMPTY;
    private List<Image> contentImages = new ArrayList<>();
    private int[] imageIds;
    private Category category;
    private String coverImage;
    private boolean hideTopImages = true;

    public boolean hasImages() {
        return CollectionUtils.isNotEmpty(contentImages);
    }

    public boolean hasImageIds() {
        return ArrayUtils.isNotEmpty(imageIds);
    }

    public void validate() {
        if (id > 0) {
            return;
        }
        if (StringUtils.isBlank(title)) {
            throw new BusinessException(String.format("Article title is blank: %s", url));
        }
        if (StringUtils.isBlank(content) && this.getContentImages().size() == 0) {
            throw new BusinessException(String.format("Article content is blank: %s -> %s", title, url));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Article article = (Article) o;
        return Objects.equal(url, article.url);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(url);
    }

    public boolean containsVideo() {
        return Tools.containsAny(this.content, "<embed", "<video", "object", "iframe");
    }
}
