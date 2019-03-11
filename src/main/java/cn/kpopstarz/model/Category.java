package cn.kpopstarz.model;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-03-11 5:04 AM
 */
public enum Category {
    General(18, "商讯"),
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