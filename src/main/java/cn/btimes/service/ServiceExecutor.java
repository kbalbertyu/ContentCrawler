package cn.btimes.service;

import cn.btimes.model.Messenger;
import cn.btimes.model.Messengers;
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
    @Inject private Messengers messengers;
    @Inject private EmailSenderHelper emailSenderHelper;

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
        sources.add(ApplicationContext.getBean(EntGroup.class));
        sources.add(ApplicationContext.getBean(CCDY.class));
        sources.add(ApplicationContext.getBean(JieMian.class));
        sources.add(ApplicationContext.getBean(QQ.class));
        sources.add(ApplicationContext.getBean(LadyMax.class));
        sources.add(ApplicationContext.getBean(LUXE.class));
        sources.add(ApplicationContext.getBean(IYiOu.class));
        sources.add(ApplicationContext.getBean(CTO51.class));
        sources.add(ApplicationContext.getBean(CNBeta.class));
    }
    public void execute() {
        messengers.clear();
        WebDriver driver = webDriverLauncher.start();
        for (Source source : sources) {
            try {
                source.execute(driver);
            } catch (Exception e) {
                String message = String.format("Error found in executing: %s", this.getClass());
                logger.error(message, e);
                Messenger messenger = new Messenger(source.getClass().getName(), message);
                this.messengers.add(messenger);
            }
        }
        if (this.messengers.isNotEmpty()) {
            this.sendMessage(this.messengers);
        }
    }

    private void sendMessage(Messengers messengers) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table>");
        for (Messenger messenger : messengers.getList()) {
            sb.append("<tr>");
            sb.append("<td>");
            sb.append(messenger.getSource());
            sb.append("</td>");
            sb.append("<td>");
            sb.append(messenger.getMessage());
            sb.append("</td>");
            sb.append("</tr>");
        }
        sb.append("</table>");
        this.emailSenderHelper.send("ContentCrawler Error Messages", sb.toString(), "tansoyu@gmail.com");
    }
}
