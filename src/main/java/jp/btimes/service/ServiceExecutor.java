package jp.btimes.service;

import cn.btimes.source.Source;
import com.amzass.service.common.ApplicationContext;
import jp.btimes.source.Agrinews;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/4/19 20:14
 */
public class ServiceExecutor extends cn.btimes.service.ServiceExecutor {

    @Override
    protected List<Source> getSources() {
        List<Source> sources = new ArrayList<>();
        sources.add(ApplicationContext.getBean(Agrinews.class));
        return sources;
    }
}
