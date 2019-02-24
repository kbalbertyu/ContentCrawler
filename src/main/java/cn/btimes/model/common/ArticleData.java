package cn.btimes.model.common;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-02-04 3:30 PM
 */
@Data
public class ArticleData {
    private String insertTime;
    private String saveTime;
    private int status;
    private String link;
    private String options;
    private int category;

    private int categoryId() {
        if (status == 3 && category > 0) {
            return category;
        }
        ArticleOptions articleOptions = JSONObject.parseObject(options, ArticleOptions.class);
        return articleOptions.firstCategory();
    }

    public Category category() {
        return Category.getById(categoryId());
    }

    public ArticleSource source() {
        return ArticleSource.getByLink(link);
    }

    public boolean published() {
        return status == 3;
    }
}
