package com.fortis.model;

import com.amzass.utils.common.RegexUtils;
import com.kber.commons.model.PrimaryKey;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.nutz.dao.entity.annotation.Column;
import org.nutz.dao.entity.annotation.Name;
import org.nutz.dao.entity.annotation.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/8/24 12:04
 */
@Data
@Table("product")
public class Product extends PrimaryKey {
    private static final Logger LOGGER = LoggerFactory.getLogger(Product.class);

    @Override
    public String getPK() {
        return asin;
    }

    @Name private String asin;
    @Column private String title;
    @Column private String url;
    @Column private String hot;
    @Column private String category;
    @Column private String subCategory = StringUtils.EMPTY;
    @Column private String image;
    @Column private float reviews;
    @Column private float stars;
    @Column private float price;
    @Column private String startDate;
    @Column private String modifiedDate;
    @Column private int times;

    public String parseAsin() {
        String asin = RegexUtils.getMatched(url, "dp\\/[a-zA-Z0-9]+");
        return StringUtils.substringAfter(asin, "/");
    }
}
