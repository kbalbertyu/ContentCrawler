package com.fortis.model;

import lombok.Data;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/8/8 14:39
 */
@Data
public class Product {
    private String title;
    private String url;
    private String image;
    private float stars;
    private int reviews;
    private float price;
    private String category;
}
