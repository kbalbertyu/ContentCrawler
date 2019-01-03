package cn.btimes.service;

import cn.btimes.source.*;
import com.amzass.service.common.ApplicationContext;
import com.google.inject.Inject;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 12/24/2018 8:33 AM
 */
public class ServiceExecutor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final static List<Source> sources = new ArrayList<>();
    @Inject private WebDriverLauncher webDriverLauncher;
    static {
        sources.add(ApplicationContext.getBean(Sina.class));
        sources.add(ApplicationContext.getBean(ThePaper.class));
        sources.add(ApplicationContext.getBean(YiCai.class));
        sources.add(ApplicationContext.getBean(NBD.class));
        sources.add(ApplicationContext.getBean(COM163.class));
        sources.add(ApplicationContext.getBean(HeXun.class));
        sources.add(ApplicationContext.getBean(LvJie.class));
        sources.add(ApplicationContext.getBean(PinChain.class));
        sources.add(ApplicationContext.getBean(GasGoo.class));
    }
    public void execute() {
        WebDriver driver = webDriverLauncher.start();
        try {
            for (Source source : sources) {
                try {
                    source.execute(driver);
                } catch (Exception e) {
                    logger.error("Error found in executing: " + this.getClass(), e);
                }
            }
        } finally {
            driver.close();
        }
    }
}
