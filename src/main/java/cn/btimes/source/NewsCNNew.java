package cn.btimes.source;

import cn.btimes.model.common.CSSQuery;
import cn.btimes.model.common.Category;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2021/3/8 15:37
 */
public class NewsCNNew extends NewsCN {
    private static final Map<String, Category> URLS = new HashMap<>();

    static {
        URLS.put("http://www.xinhuanet.com/fortune/gd.htm", Category.ECONOMY);
        URLS.put("http://www.xinhuanet.com/fortune/", Category.ECONOMY);
        URLS.put("http://www.xinhuanet.com/house/index.htm", Category.REALESTATE);
        URLS.put("http://www.xinhuanet.com/tech/index.htm", Category.TECH);
        URLS.put("http://www.xinhuanet.com/money/index.htm", Category.FINANCE);
    }

    @Override
    protected Map<String, Category> getUrls() {
        return URLS;
    }

    @Override
    protected CSSQuery getCSSQuery() {
        return new CSSQuery(".xpage-content-list > li", "#detail", ".tit > a", "",
            "", "");
    }
}
