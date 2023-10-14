package az.risk.agentx.service;

import az.risk.agentx.dto.AgentDto;
import az.risk.agentx.dto.ChangeStateDto;
import az.risk.agentx.model.ReasonCode;
import az.risk.agentx.model.user.User;

import java.util.List;

public interface UserService {

    User getUser(String username, String password);
    int signIn(int extension);

    int singOut(String reasonCodeId);

    AgentDto changeState(ChangeStateDto changeStateDto);

    List<ReasonCode> getReasonCodeListByCategory(String category);
}
