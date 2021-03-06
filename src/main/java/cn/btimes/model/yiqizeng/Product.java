package cn.btimes.model.yiqizeng;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.File;
import java.util.Date;
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
    @JSONField(serialize = false)
    private List<File> galleryFiles;
    @JSONField(serialize = false)
    private List<File> contentGalleryFiles;
    private Date dateUpdated;

    public String formUrl() {
        return String.format("/goodsPurchase/toCommoditydetails?goodID=%d&groupType=%d&btnMember=1", id, groupType);
    }
}
