package cn.btimes.model.baidu;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/8/1 15:09
 */
public enum BaiduLink {
    PC(""),
    AMP("amp"),
    MIP("mip");

    BaiduLink(String type) {
        this.type = type;
    }

    public String postUrl(String site, String token) {
        return String.format(POST_URL_FORMAT, site, token, type);
    }

    public static final String API_HOST = "data.zz.baidu.com";
    private static final String POST_URL_FORMAT = "http://data.zz.baidu.com/urls?site=%s&token=%s&type=%s";
    public final String type;
}
