package cn.btimes.model.common;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-01 9:14 PM
 */
public enum Category {
    TECH(24, "科技"),
    CHINA(55, "中国"),
    ECONOMY(57, "经济"),
    FINANCE(58, "金融"),
    COMPANY(59, "公司"),
    SPORTS(61, "体育"),
    ENTERTAINMENT(60, "娱乐"),
    LIFESTYLE(62, "时尚生活"),
    REALESTATE(63, "房地产"),
    AUTO(64, "汽车"),
    TRAVEL(65, "旅游"),
    FUTURE_INDUSTRIES(68, "未来产业"),
    /** Undetermined category **/
    BLANK(0, "");

    public final int id;
    public final String title;

    Category(int id, String title) {
        this.id = id;
        this.title = title;
    }

    public static Category getById(int id) {
        for (Category category : Category.values()) {
            if (category.id == id) {
                return category;
            }
        }
        return BLANK;
    }
}
