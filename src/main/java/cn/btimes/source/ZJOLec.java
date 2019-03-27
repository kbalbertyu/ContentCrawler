package cn.btimes.source;

import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-03-26 10:02 PM
 */
public class ZJOLec extends ZJOLbiz {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://ec.zjol.com.cn/ezx/", Category.ECONOMY);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery("#pageContent > .textList", ".artCon", "h3 > a", "",
            "", "h3 > span");
    }
}
