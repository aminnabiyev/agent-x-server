package az.risk.agentx.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AgentStateEvent {
    private String statusName;
    private int descriptionCode;
    private String ciscoOperatorId;
    private String createDate;
}
