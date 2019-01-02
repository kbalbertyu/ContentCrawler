package cn.btimes.model;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-01 9:14 PM
 */
public enum Category {
    TECH(24),
    CHINA(55),
    ECONOMY(57),
    FINANCE(58),
    COMPANY(59),
    SPORTS(61),
    ENTERTAINMENT(60),
    LIFESTYLE(62),
    REALESTATE(63),
    AUTO(64),
    TRAVEL(65),
    FUTURE_INDUSTRIES(68),
    /** Undetermined category **/
    BLANK(0);

    public final int id;

    Category(int id) {
        this.id = id;
    }
}
