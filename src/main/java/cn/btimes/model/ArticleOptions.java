package cn.btimes.model;

import lombok.Data;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-02-13 9:25 AM
 */
@Data
class ArticleOptions {
    private int[] ar_cat;

    int firstCategory() {
        if (ar_cat == null || ar_cat.length == 0) {
            return 0;
        }
        return ar_cat[0];
    }
}
