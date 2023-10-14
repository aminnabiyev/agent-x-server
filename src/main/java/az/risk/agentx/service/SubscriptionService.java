package az.risk.agentx.service;

import az.risk.agentx.dto.AgentDto;

public interface SubscriptionService {
    AgentDto subscribe(int extension);
    void unsubscribe(String reasonCodeId);
}
