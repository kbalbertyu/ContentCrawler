package jp.btimes.service;

import cn.btimes.source.Source;
import com.amzass.service.common.ApplicationContext;
import jp.btimes.source.*;

import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2020/4/19 20:14
 */
public class ServiceExecutor extends cn.btimes.service.ServiceExecutor {

    @Override
    protected List<Source> getSources() {
        // Sources for CN edition, borrow from BTCN
        List<Source> sources = this.getBTCNSources();
        List<Source> usedSources = this.getBTCNUsedSources();
        sources.addAll(usedSources);

        // Sources for JP edition
        sources.add(ApplicationContext.getBean(KyotoNP.class));
        sources.add(ApplicationContext.getBean(Fukuishimbun.class));
        sources.add(ApplicationContext.getBean(Kanaloco.class));
        sources.add(ApplicationContext.getBean(JomoNews.class));
        sources.add(ApplicationContext.getBean(Jacom.class));
        sources.add(ApplicationContext.getBean(IgakuShoin.class));
        sources.add(ApplicationContext.getBean(Hokkoku.class));
        sources.add(ApplicationContext.getBean(GifuNp.class));
        sources.add(ApplicationContext.getBean(Chunichi.class));
        sources.add(ApplicationContext.getBean(Bci.class));
        sources.add(ApplicationContext.getBean(Asahi.class));
        sources.add(ApplicationContext.getBean(Agrinews.class));
        return sources;
    }
}
