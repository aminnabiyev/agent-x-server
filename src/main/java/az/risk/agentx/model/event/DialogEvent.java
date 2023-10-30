package az.risk.agentx.model.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DialogEvent {
    private String id;
    private String callType;
    private String callerState;
    private String calledState;
    private String fromAddress;
    private String toAddress;
    private String username;
    private String startTime;

}
