package az.risk.agentx.service;

import az.risk.agentx.model.event.AgentStateEvent;

public interface NotificationService {
    void newCall(String sessionId, String incomingNumber, String startDate, String operator);
    void missedCall(String sessionId);

    void answeredCall(String sessionId);
    void endedCall(String sessionId);

    void agentStateChanged(AgentStateEvent event);
}
