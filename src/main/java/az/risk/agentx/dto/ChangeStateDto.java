package az.risk.agentx.dto;


import az.risk.agentx.model.user.AgentState;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;


public record ChangeStateDto(@NotNull String state,
                             @NotNull int reasonCodeId) {
}
