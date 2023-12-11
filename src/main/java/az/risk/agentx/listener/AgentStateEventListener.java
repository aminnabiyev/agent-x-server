package az.risk.agentx.listener;

import az.risk.agentx.model.event.AgentStateEvent;
import az.risk.agentx.service.NotificationService;
import az.risk.agentx.util.XmlToJavaConverter;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Log4j2
@RequiredArgsConstructor
public class AgentStateEventListener implements ItemEventListener<Item> {

    private final NotificationService notificationService;
    private final String username;

    @Override
    public void handlePublishedItems(ItemPublishEvent<Item> itemsPublished) {

        log.info("Received notification : {}", itemsPublished.getItems().size());

        for (Item item : itemsPublished.getItems()) {
            extractStateChangeFromItem(item);
        }

    }

    private void extractStateChangeFromItem(Item item) {

        log.trace("start to extract");

        JsonNode rootNode = XmlToJavaConverter.parseXmlToJsonNode(item.toXML());
        log.trace("Agent state event for {} {}", username, rootNode);
        try {
            JsonNode dataNode = rootNode != null ? rootNode.get("notification").get("Update").get("data") : null;
            JsonNode apiErrors = dataNode != null ? dataNode.get("apiErrors") : null;
            JsonNode user = dataNode != null ? dataNode.get("user") : null;
            if (apiErrors != null) {
                var peripheralErrorTextNode = apiErrors.get("apiError").get("peripheralErrorText");
                var errorMessage = peripheralErrorTextNode != null ? peripheralErrorTextNode.asText() : apiErrors.get("apiError").get("errorType").asText();
                log.error(errorMessage);
            } else if (user != null) {
                {
                    var state = user.get("state").asText();
                    var reasonCodeIdNode = user.get("reasonCodeId");
                    var reasonCodeId = reasonCodeIdNode != null ? reasonCodeIdNode.asInt() : 0;
                    var stateChangeTime = user.get("stateChangeTime").asText();
                    notificationService.agentStateChanged(new AgentStateEvent(state, reasonCodeId, username, stateChangeTime));
                }
            }
        } catch (Exception e) {
            log.error("Error occurred while getting notification {}", e.getMessage());
            log.catching(Level.ERROR, e);
        }

    }
}
