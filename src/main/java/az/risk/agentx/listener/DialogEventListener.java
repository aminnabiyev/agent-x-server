package az.risk.agentx.listener;

import az.risk.agentx.util.XmlToJavaConverter;
import az.risk.agentx.model.DialogEvent;
import az.risk.agentx.service.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;

import java.util.ArrayList;
import java.util.List;

@Log4j2
@RequiredArgsConstructor
public class DialogEventListener implements ItemEventListener<Item> {

    private final NotificationService callService;
    private final String username;
    private final int extension;

    @Override
    public void handlePublishedItems(ItemPublishEvent<Item> itemsPublished) {

        log.info("Received Dialog event for {} {}", username, extension);

        DialogEvent event = null;
        for (Item item : itemsPublished.getItems()) {
            event = extractMessageFromItem(item);
            log.info("Received Event for {} {}, {}", username, extension, event);
        }
        if (event != null) {
            switch (event.getCalledState()) {
                case "ALERTING" ->
                        callService.newCall(event.getId(), String.valueOf(event.getFromAddress()), event.getStartTime(), username);
                case "DROPPED" -> {
                    switch (event.getCallerState()) {
                        case "ACTIVE" -> callService.endedCall(event.getId());
                        case "INITIATED" -> callService.missedCall(event.getId());
                        default -> {
                            if (event.getCallType().equals("OUT")) callService.missedCall(event.getId());
                            else callService.endedCall(event.getId());
                        }
                    }
                }
                case "ACTIVE" -> {
                    if (event.getCallerState().equals("ACTIVE") && !event.getCallType().equals("OUT")) {
                        callService.answeredCall(event.getId());
                    }
                }
            }
        }


    }

    private DialogEvent extractMessageFromItem(Item item) {
        JsonNode rootNode = XmlToJavaConverter.parseXmlToJsonNode(item.toXML());
        try {
            var updateNode = rootNode != null ? rootNode.get("notification").get("Update") : null;
            var dataNode = updateNode != null ? updateNode.get("data") : null;
            var dialogsNode = dataNode != null ? dataNode.get("dialogs") : null;
            var dialogNode = dialogsNode != null ? dialogsNode.get("Dialog") : dataNode != null ? dataNode.get("dialog") : null;
            log.trace("Dialog event for {} {}", username, dialogNode);
            if (dialogNode != null) {
                var idNode = dialogNode.get("id");
                var fromAddressNode = dialogNode.get("fromAddress");
                var toAddressNode = dialogNode.get("toAddress");
                var mediaPropertiesNode = dialogNode.get("mediaProperties");
                var dnisNode = mediaPropertiesNode != null ? mediaPropertiesNode.get("DNIS") : null;
                var dnis = dnisNode != null ? dnisNode.asInt() : 0;
                var callTypeNode = mediaPropertiesNode != null ? mediaPropertiesNode.get("callType") : null;
                var id = idNode != null ? idNode.asText() : "";
                var fromAddress = fromAddressNode != null ? fromAddressNode.asInt() : 0;
                var toAddress = toAddressNode != null ? toAddressNode.asInt() : 0;
                if (dnis == 0 || dnis != extension) return null;
                var callType = callTypeNode != null ? callTypeNode.asText() : "";
                var participantsNode = dialogNode.get("participants").get("Participant");
                JsonNode callerParticipantNode = null;
                JsonNode calledParticipantNode = null;
                if (participantsNode.isArray()) {
                    var participantsNodeIterator = participantsNode.iterator();
                    List<JsonNode> participants = new ArrayList<>();

                    while (participantsNodeIterator.hasNext()) {
                        participants.add(participantsNodeIterator.next());
                    }
                    callerParticipantNode = participants.stream().filter(p -> p.get("mediaAddress").asInt(1) == fromAddress).findAny().orElse(null);
                    calledParticipantNode = participants.stream().filter(p -> p.get("mediaAddress").asInt(1) == dnis).findAny().orElse(null);

                } else {
                    if (participantsNode.get("mediaAddress").asInt() == fromAddress) {
                        callerParticipantNode = participantsNode;
                    } else if (participantsNode.get("mediaAddress").asInt() == toAddress) {
                        calledParticipantNode = participantsNode;
                    }
                }
                var callerStateNode = callerParticipantNode != null ? callerParticipantNode.get("state") : null;
                var callingStateNode = calledParticipantNode != null ? calledParticipantNode.get("state") : null;

                var callerState = callerStateNode != null ? callerStateNode.asText() : "";
                var callingState = callingStateNode != null ? callingStateNode.asText() : "";
                var startTime = calledParticipantNode != null ? calledParticipantNode.get("startTime").asText() : "";
                log.trace("returning");

                return new DialogEvent(id, callType, callerState, callingState, fromAddress, toAddress, username, startTime);
            }

        } catch (Exception e) {
            log.error("Error occurred during parsing notification");
            log.error(e.getMessage());
        }
        return null;
    }

}
