package cn.btimes.source;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-03-27 8:36 AM
 */
public abstract class SourceWithoutDriver extends Source {

    @Override
    boolean withoutDriver() {
        return true;
    }
}
