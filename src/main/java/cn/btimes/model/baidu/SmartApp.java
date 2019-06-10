package cn.btimes.model.baidu;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019/6/10 9:25
 */
public enum SmartApp {
    SPORTS(16384334),
    ENTERTAINMENT(16384491);

    SmartApp(int id) {
        this.id = id;
    }
    public final int id;
}
