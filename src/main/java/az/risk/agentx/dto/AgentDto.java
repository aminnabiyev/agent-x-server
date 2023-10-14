package az.risk.agentx.dto;

import az.risk.agentx.model.user.AgentState;

public record AgentDto (String username, AgentState state){
}
