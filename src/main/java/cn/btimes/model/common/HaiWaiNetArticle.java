package cn.btimes.model.common;

import lombok.Data;

import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/8/12 6:38
 */
@Data
public class HaiWaiNetArticle {
    private int id;
    private String body;
    private String link;
    private String title;
    private List<HWNGallery> galleries;
}
