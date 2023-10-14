package az.risk.agentx.service;


public interface CallService {
    void makeCall(String toAddress);
    void answerCall(String callId);
    void transferCall(String callId, String toAddress);

    void conferenceCall(String callId, String toAddress);
    void endCall(String callId);

}
