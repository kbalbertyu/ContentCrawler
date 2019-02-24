package cn.btimes.service;

import cn.btimes.model.common.SMTPContext;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amzass.enums.common.ConfigEnums.AccountType;
import com.amzass.model.common.Account;
import com.amzass.service.common.EmailSender.EmailContentType;
import com.amzass.utils.common.Constants;
import com.amzass.utils.common.Exceptions.AuthenticationFailException;
import com.amzass.utils.common.Exceptions.BusinessException;
import com.amzass.utils.common.Tools;
import com.mailman.service.common.ServiceEmailHelper;
import com.mailman.utils.ServiceUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-21 10:32 PM
 */
public class EmailSenderHelper extends ServiceEmailHelper {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Account sender = this.initEmailSender();

    private Account initEmailSender() {
        String emailPassword = Tools.getCustomizingValue("QQ_EMAIL_PASSWORD");
        Account account = new Account(emailPassword, AccountType.EmailSender);
        account.setName(Tools.getCustomizingValue("EMAIL_SENDER_NAME"));
        return account;
    }

    void send(String title, String content, String... receiverEMail) {
        this.send(title, content, EmailContentType.Html, new Destination().withToAddresses(receiverEMail));
    }

    private void send(String title, String content, EmailContentType contentType, Destination destination) {
        String emailsText = ServiceUtils.abbrev(destination);
        String errorMsg = null;
        long start = System.currentTimeMillis();
        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            try {
                this.sendEmail(title, content, contentType, destination);
                logger.info("使用Mailing API发送邮件给{}成功完成，耗时{}", emailsText, Tools.formatCostTime(start));
                return;
            } catch (AuthenticationFailException e) {
                logger.warn("Seller邮箱{}密码可能设置有误，发送终止", sender);
                errorMsg = Tools.getExceptionMsg(e);
                break;
            } catch (EmailException e) {
                logger.error("第{}次基于Mailing API发送邮件给{}失败，尝试重复发送: ", i + 1, emailsText, e);
                errorMsg = Tools.getExceptionMsg(e);
            }
        }

        throw new BusinessException(String.format("Failed to send email to %s after %s attempts via Mailing API: %s",
            emailsText, Constants.MAX_REPEAT_TIMES, StringUtils.defaultString(errorMsg)));
    }

    private void sendEmail(String title, String content, EmailContentType contentType, Destination destination) throws EmailException {
        Email email = this.prepareEmailContext(title, content, contentType, destination);
        this._sendEmail(email);
    }

    private void _sendEmail(Email email) throws EmailException {
        try {
            if (email.getMimeMessage() == null) {
                email.buildMimeMessage();
            }
            MimeMessage message = email.getMimeMessage();
            SMTPContext ctx = this.initEmailTransport();
            ctx.getTransport().sendMessage(message, message.getAllRecipients());
        } catch (javax.mail.AuthenticationFailedException e) {
            throw new AuthenticationFailException(e);
        } catch (MessagingException e) {
            throw new EmailException(e.getMessage());
        }
    }

    private SMTPContext initEmailTransport() throws MessagingException {
        long start = System.currentTimeMillis();
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.qq.com");
        props.put("mail.smtp.port", 465);
        props.put("mail.smtp.ssl.enable", true);
        props.put("mail.smtp.auth", true);

        String email = sender.getEmail();
        Session session = Session.getInstance(props, new DefaultAuthenticator(email, sender.getPassword()));
        Transport transport = session.getTransport("smtp");
        transport.connect();

        SMTPContext ctx = new SMTPContext(email, session, transport);
        logger.info("{} - Email SMTP连接完成, 耗时{}.", sender.getEmail(), Tools.formatCostTime(start));
        return ctx;
    }

    private Email prepareEmailContext(String title, String content, EmailContentType contentType, Destination destination) throws EmailException {
        Email email = contentType == EmailContentType.Html ? new HtmlEmail() : new SimpleEmail();
        email.setCharset(Constants.UTF8);
        email.setFrom(sender.getEmail(), sender.getName());
        if (CollectionUtils.isNotEmpty(destination.getToAddresses())) {
            for (String recipient : destination.getToAddresses()) {
                email.addTo(recipient);
            }
        }
        if (CollectionUtils.isNotEmpty(destination.getCcAddresses())) {
            for (String recipient : destination.getCcAddresses()) {
                email.addCc(recipient);
            }
        }
        email.setSubject(title);
        if (contentType == EmailContentType.Html) {
            ((HtmlEmail) email).setHtmlMsg(content);
        } else {
            email.setMsg(content);
        }
        email.setHostName(Constants.LOCALHOST);
        return email;
    }
}
