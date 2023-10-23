package az.risk.agentx.util.xmpp;

import az.risk.agentx.exception.AgentXConnectionFailedException;
import az.risk.agentx.listener.AgentStateEventListener;
import az.risk.agentx.listener.DialogEventListener;
import az.risk.agentx.service.NotificationService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.pubsub.Node;
import org.jivesoftware.smackx.pubsub.PubSubManager;

@Data
@Log4j2
@NoArgsConstructor
@AllArgsConstructor
public class PubSubConnection {
    private PubSubManager pubSubManager;
    private XMPPConnection connection;
    private DialogEventListener dialogEventListener;
    private AgentStateEventListener agentStateEventListener;

    private static final String HOSTNAME = "uccx01.zbaz.local";

    @SuppressWarnings("VulnerableCodeUsages")
    private static final ConnectionConfiguration CONFIG = new ConnectionConfiguration(HOSTNAME, 5222);

    static {
        CONFIG.setSASLAuthenticationEnabled(true);
        CONFIG.setReconnectionAllowed(false);
    }


    public PubSubConnection(NotificationService notificationService, String username, String password, int extension) {
        log.trace("Creating new connection for {}", username);
        var connection = new XMPPConnection(CONFIG);
        try {
            connection.connect();
            log.trace("Connected");
            log.trace("Logging in");
            connection.login(username, password, username);
            log.trace("Logged in");
            this.pubSubManager = new PubSubManager(connection, "pubsub." + HOSTNAME);
            this.connection = connection;
            this.dialogEventListener = new DialogEventListener(notificationService, username, extension);
            this.agentStateEventListener = new AgentStateEventListener(notificationService, username);
            subscribeToDialogEvents(username);
            subscribeToUserEvents(username);
            log.trace("Connection with id created {} for {}", connection.getConnectionID(), username);
        } catch (XMPPException e) {
            log.error("Xmpp connection failed for {}, Exception: {}", username, e.getMessage());
            log.catching(Level.ERROR, e);
            log.error(e);
            log.trace("Throwing AgentXConnectionFailedException");
            throw new AgentXConnectionFailedException("AgentX Connection Failed");
        }

    }

    public void connect() {

        log.trace("Connection called while connection with id {} is {} connected", connection.getConnectionID(), connection.isConnected() ? "" : "not");

        if (!connection.isConnected()) {
            log.trace("Connecting {}", connection.getConnectionID() );
            try {
                connection.connect();
            } catch (XMPPException e) {
                log.error("Xmpp connection failed {}", e.getMessage());
                log.catching(Level.ERROR, e);
                log.trace("Throwing AgentXConnectionFailedException");
                throw new AgentXConnectionFailedException("AgentX Connection Failed");
            }
        }
        log.trace("Connection with id {} is {} connected", connection.getConnectionID(), connection.isConnected() ? "" : "not");
    }

    public void disconnect() {
        log.trace("Disconnecting " + connection.getConnectionID());
        if (isConnected()) {
            connection.disconnect();
            log.trace("Disconnected");
        } else {
            log.trace("Not connected");
        }
    }

    public boolean isConnected() {
        log.trace("isConnected called while connection with id {}. Returning {}", connection.getConnectionID(), connection.isConnected());
        return connection.isConnected();
    }


    public void subscribeToDialogEvents(String username) {

        log.trace("Subscribe to Dialog events init");

        var nodeId = "/finesse/api/User/%s/Dialogs".formatted(username);
        log.trace("Start to subscribe node {}", nodeId);

        Node node;
        try {
            node = pubSubManager.getNode(nodeId);
        } catch (XMPPException e) {
            log.info("Can not get node from PubSubManager");
            log.trace("Throwing new AgentXConnectionFailedException");
            throw new AgentXConnectionFailedException("Can not subscribe to Agentx Notification Service");
        }
        log.trace("Adding Dialog event listener");
        node.addItemEventListener(this.dialogEventListener);
        log.trace("Added Dialog event listener");
    }
    public void subscribeToUserEvents(String username) {

        log.trace("Subscribe to User events init");

        var nodeId = "/finesse/api/User/%s".formatted(username);
        log.trace("Start to subscribe node {}", nodeId);

        Node node;
        try {
            node = pubSubManager.getNode(nodeId);
        } catch (XMPPException e) {
            log.info("Can not get node from PubSubManager");
            log.trace("Throwing new AgentXConnectionFailedException");
            throw new AgentXConnectionFailedException("Can not subscribe to Agentx Notification Service");
        }
        log.trace("Adding Agent State event listener");
        node.addItemEventListener(this.agentStateEventListener);
        log.trace("Added Agent State event listener");
    }

}

