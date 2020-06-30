package cn.btimes.model.news;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/6/29 12:03
 */
public enum NewsFlowSource {
    USHK("ushknews.com");

    public final String source;

    NewsFlowSource(String source) {
        this.source = source;
    }
}
