package az.risk.agentx.controller;

import az.risk.agentx.model.Response;
import az.risk.agentx.service.CallService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequiredArgsConstructor
@RequestMapping("call")
public class CallController {

    private final CallService callService;

    @PutMapping("/{callId}/answer")
    public ResponseEntity<?> answerCall(@PathVariable("callId") String callId) {
        return ResponseEntity.ok(
                Response.builder()
                        .timeStamp(LocalDateTime.now())
                        .status(OK.value())
                        .message("Call answered")
                        .data(callService.answerCall(callId))
                        .build());
    }

    @DeleteMapping("/{callId}/end")
    public ResponseEntity<?> endCall(@PathVariable("callId") String callId) {
        return ResponseEntity.ok(
                Response.builder()
                        .timeStamp(LocalDateTime.now())
                        .status(OK.value())
                        .message("Call ended")
                        .data(callService.endCall(callId))
                        .build());
    }

}
