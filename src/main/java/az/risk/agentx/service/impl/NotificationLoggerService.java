package az.risk.agentx.service.impl;

import az.risk.agentx.service.NotificationService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class NotificationLoggerService implements NotificationService {

    @Override
    public void newCall(String sessionId, String incomingNumber, String startDate, String operator) {
        log.info("New Call - sessionId: {}, incomingNumber: {}, startDate: {}, operator: {}", sessionId, incomingNumber, startDate, operator);
    }

    @Override
    public void missedCall(String sessionId) {
        log.info("Missed call {}", sessionId);
    }

    @Override
    public void answeredCall(String sessionId) {
        log.info("Answered call {}", sessionId);
    }

    @Override
    public void endedCall(String sessionId) {
        log.info("Ended call {}", sessionId);
    }
}
