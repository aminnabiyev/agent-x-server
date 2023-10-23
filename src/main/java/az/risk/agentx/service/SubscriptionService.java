package az.risk.agentx.service;

import az.risk.agentx.dto.AgentDto;

public interface SubscriptionService {
    AgentDto subscribe(int extension);
    AgentDto unsubscribe(String reasonCodeId);
}
