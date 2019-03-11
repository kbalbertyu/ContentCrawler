package cn.kpopstarz.source;

import cn.btimes.model.common.Category;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-03-11 4:57 PM
 */
public class Sina extends cn.btimes.source.Sina {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("https://ent.sina.com.cn/korea/", Category.General);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected int getSourceId() {
        return 13;
    }
}
