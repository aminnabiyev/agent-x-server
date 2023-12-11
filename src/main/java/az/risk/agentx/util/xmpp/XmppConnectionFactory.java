package az.risk.agentx.util.xmpp;

import az.risk.agentx.model.user.User;
import az.risk.agentx.service.NotificationService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.context.SecurityContextHolder;
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
            if (connections.get(username).isConnected()) {
                log.trace("Connection already connected");
                return;
            } else {
                log.trace("Connection exists but not connected");
                log.trace("Removing disconnected connection");
                connections.remove(username);
                log.trace("Disconnected connection removed");
            }

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
            log.trace("Removing disconnected connection");
            connections.remove(username);
            log.trace("Disconnected connection removed");
        } else {
            log.trace("Connection doesn't exist");
        }
        System.out.println("After disconnect " + connections.size());


        log.info("Final connection status is {} connected", connection == null ? "not" : connection.isConnected() ? "" : "not");
    }

    public boolean isConnected(String username) {
        if (connections.containsKey(username)) {
            log.trace("Connection exist");
           if(connections.get(username).isConnected()){
               log.trace("Connection exist and connected");
               log.trace("returning true");
               return true;
           } else {
               log.trace("Connection exist but  not connected");
               var loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
               log.trace("Logged in user is {}", loggedInUser);
               log.trace("Call connect method");
               connect(username, loggedInUser.getPassword(), loggedInUser.getExtension());
           }
           return isConnected(username);
        }
        return false;
    }

    public void disc(String username){
        log.trace("DISC init");
        PubSubConnection connection = null;
        if (connections.containsKey(username)) {
            log.trace("Connection exist");
            connection = connections.get(username);
            connection.disconnect();
        } else {
            log.trace("Connection doesn't exist");
        }
        log.info("Final connection status is {} connected", connection == null ? "not" : connection.isConnected() ? "" : "not");
    }


    @PreDestroy
    private void destroy() {
        log.info("Destroy");
        connections.forEach((k, v) -> v.disconnect());
    }

}
