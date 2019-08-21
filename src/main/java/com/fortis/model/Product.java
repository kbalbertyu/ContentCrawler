package com.fortis.model;

import com.amzass.utils.common.RegexUtils;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/8/8 14:39
 */
@Data
public class Product {
    private String title;
    private String url;
    private String asin;
    private String image;
    private float stars;
    private int reviews;
    private float price;
    private String category;

    public String parseAsin() {
        String asin = RegexUtils.getMatched(url, "dp\\/[a-zA-Z0-9]+");
        return StringUtils.substringAfter(asin, "/");
    }
}
