package az.risk.agentx.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Call {
    private String sessionId;
    private String incomingNumber;
    private String startDate;
    private String operator;
}
