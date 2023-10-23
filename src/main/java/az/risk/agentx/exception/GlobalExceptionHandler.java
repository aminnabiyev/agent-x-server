package az.risk.agentx.exception;

import az.risk.agentx.model.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

import static org.springframework.http.HttpStatus.*;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({FinesseApiRequestFailedException.class})
    public ResponseEntity<Object> handleFinesseApiRequestFailedException(FinesseApiRequestFailedException exception) {
        return ResponseEntity.status(BAD_REQUEST).body(
                Response.builder()
                        .timeStamp(LocalDateTime.now())
                        .status(BAD_REQUEST.value())
                        .error(BAD_REQUEST)
                        .message(exception.getMessage())
                        .build());
    }
    @ExceptionHandler({AgentStateException.class})
    public ResponseEntity<Object> handleAgentStateException(AgentStateException exception) {
        return ResponseEntity.status(FORBIDDEN).body(
                Response.builder()
                        .timeStamp(LocalDateTime.now())
                        .status(FORBIDDEN.value())
                        .error(FORBIDDEN)
                        .message(exception.getMessage())
                        .build());
    }

    @ExceptionHandler({InvalidCallActionException.class})
    public ResponseEntity<Object> handleInvalidCallActionException(InvalidCallActionException exception) {
        return ResponseEntity.status(BAD_REQUEST).body(
                Response.builder()
                        .timeStamp(LocalDateTime.now())
                        .status(BAD_REQUEST.value())
                        .error(BAD_REQUEST)
                        .message(exception.getMessage())
                        .build());
    }    @ExceptionHandler({AgentXConnectionFailedException.class})
    public ResponseEntity<Object> handleAgentXSubscriptionFailedException(AgentXConnectionFailedException exception) {
        return ResponseEntity.status(BAD_REQUEST).body(
                Response.builder()
                        .timeStamp(LocalDateTime.now())
                        .status(BAD_REQUEST.value())
                        .error(BAD_REQUEST)
                        .message(exception.getMessage())
                        .build());
    }
    @ExceptionHandler({RuntimeException.class})
    public ResponseEntity<Object> handleRuntimeException(RuntimeException exception) {
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(
                Response.builder()
                        .timeStamp(LocalDateTime.now())
                        .status(INTERNAL_SERVER_ERROR.value())
                        .error(INTERNAL_SERVER_ERROR)
                        .message(exception.getMessage())
                        .build());
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<Object> handleException(Exception exception) {
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(
                Response.builder()
                        .timeStamp(LocalDateTime.now())
                        .status(INTERNAL_SERVER_ERROR.value())
                        .error(INTERNAL_SERVER_ERROR)
                        .message(exception.getMessage())
                        .build());
    }

}
