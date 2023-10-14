package az.risk.agentx.util.xmpp;

import az.risk.agentx.service.NotificationService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Log4j2
@Component
@RequiredArgsConstructor
public class XmppConnectionFactory {
    private final Map<String, PubSubConnection> connections = new HashMap<>();

    private final NotificationService notificationService;


    public void connect(String username, String password, int extension) {

        log.trace("Xmpp connection init for {}", username);

        if (connections.containsKey(username)) {
            log.trace("Connection already exist");
            return;
        }

        var connection = new PubSubConnection(notificationService, username, password, extension);
        connections.put(username, connection);

        log.info("Final connection status is {} connected", connection.isConnected() ? " " : "not");
    }

    public void disconnect(String username) {
        log.trace("Xmpp disconnection init");
        PubSubConnection connection = null;
        if (connections.containsKey(username)) {
            log.trace("Connection exist");
            connection = connections.get(username);
            connection.disconnect();
            connections.remove(username);
        } else {
            log.trace("Connection doesn't exist");
        }
        log.info("Final connection status is {} connected", connection == null ? "not" : connection.isConnected() ? "" : "not");
    }

    public boolean isConnected(String username) {
        if (connections.containsKey(username)) {
            return connections.get(username).isConnected();
        }
        return false;
    }


    @PreDestroy
    private void destroy() {
        log.info("Destroy");
        connections.forEach((k, v) -> v.disconnect());
    }

}
