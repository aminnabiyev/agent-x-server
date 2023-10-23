package az.risk.agentx.service;


import az.risk.agentx.dto.CallDto;

public interface CallService {
    void makeCall(String toAddress);
    CallDto answerCall(String callId);
    void transferCall(String callId, String toAddress);

    void conferenceCall(String callId, String toAddress);
    CallDto endCall(String callId);

}
