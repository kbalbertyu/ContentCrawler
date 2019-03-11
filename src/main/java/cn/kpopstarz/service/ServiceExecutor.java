package cn.kpopstarz.service;

import cn.btimes.source.Source;
import cn.kpopstarz.source.ReBo;
import com.amzass.service.common.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-03-11 5:09 AM
 */
public class ServiceExecutor extends cn.btimes.service.ServiceExecutor {

    @Override
    protected List<Source> getSources() {
        List<Source> sources = new ArrayList<>();
        sources.add(ApplicationContext.getBean(ReBo.class));
        return sources;
    }

    @Override
    protected void statistic() {
    }

    @Override
    protected void syncSavedArticles() {
    }
}
