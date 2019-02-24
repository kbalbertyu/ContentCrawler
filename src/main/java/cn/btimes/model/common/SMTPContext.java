package cn.btimes.model.common;

import javax.mail.Session;
import javax.mail.Transport;

/**
 * @author <a href="mailto:kbalbertyu@gmail.com">Albert Yu</a> 2019-01-21 11:13 PM
 */
public class SMTPContext {
    public boolean valid() {
        return session != null && transport != null && transport.isConnected();
    }

    public SMTPContext(String email, Session session, Transport transport) {
        this.email = email;
        this.session = session;
        this.transport = transport;
    }
    private final String email;
    private final Session session;
    private final Transport transport;

    public Transport getTransport() {
        return transport;
    }
    public String getEmail() {
        return email;
    }
}
