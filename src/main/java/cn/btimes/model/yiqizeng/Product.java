package cn.btimes.model.yiqizeng;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/6/17 23:53
 */
@Data
public class Product {
    private int id;
    private int groupType;
    private String title;
    private String titleNote;
    private String description;
    private float standardPrice;
    private float vipPrice;
    private float marketPrice;
    private int soldCount;
    private int inventory;
    private Map<String, List<String>> specs;
    private List<String> gallery;
    private List<String> contentGallery;

    public String formUrl() {
        return String.format("/goodsPurchase/toCommoditydetails?goodID=%d&groupType=%d&btnMember=1", id, groupType);
    }
}
