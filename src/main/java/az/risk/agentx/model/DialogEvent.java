package az.risk.agentx.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DialogEvent {
    private String id;
    private String callType;
    private String callerState;
    private String calledState;
    private int fromAddress;
    private int toAddress;
    private String username;
    private String startTime;

    @Override
    public String toString() {
        return "DialogEvent{" +
                "id='" + id + '\'' +
                ", callType='" + callType + '\'' +
                ", callerState='" + callerState + '\'' +
                ", calledState='" + calledState + '\'' +
                ", fromAddress=" + fromAddress +
                ", toAddress=" + toAddress +
                ", username='" + username + '\'' +
                ", startTime='" + startTime + '\'' +
                '}';
    }

}
