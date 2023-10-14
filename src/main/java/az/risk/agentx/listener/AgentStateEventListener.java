package az.risk.agentx.listener;

import az.risk.agentx.util.XmlToJavaConverter;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Getter
@Log4j2
@Service
public class AgentStateEventListener implements ItemEventListener<Item> {

    private final CompletableFuture<String> messageReceived = new CompletableFuture<>();

    @Override
    public void handlePublishedItems(ItemPublishEvent<Item> itemsPublished) {

        log.info("Received notification : {}", itemsPublished.getItems().size());

        for (Item item : itemsPublished.getItems()) {
            String message = extractMessageFromItem(item);
            messageReceived.complete(message);
        }

    }

    private String extractMessageFromItem(Item item) {
        log.trace("start to extract");

        String message = null;

        JsonNode rootNode = XmlToJavaConverter.parseXmlToJsonNode(item.toXML());
        try {
            JsonNode dataNode = rootNode != null ? rootNode.get("notification").get("Update").get("data") : null;
            JsonNode apiErrors = dataNode != null ? dataNode.get("apiErrors") : null;
            JsonNode user = dataNode != null ? dataNode.get("user") : null;
            if (apiErrors != null) {
                var peripheralErrorTextNode = apiErrors.get("apiError").get("peripheralErrorText");
                message = peripheralErrorTextNode != null ? peripheralErrorTextNode.asText() : apiErrors.get("apiError").get("errorType").asText();
            } else if (user != null) message = user.get("reasonCode").get("label").asText();
        } catch (Exception e) {
            log.error("Error occurred while getting notification {}", e.getMessage());
            log.catching(Level.ERROR, e);
        }


        return message;
    }
}
