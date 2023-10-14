package az.risk.agentx.controller;

import az.risk.agentx.service.CallService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("call")
public class CallController {

    private final CallService callService;

    @GetMapping("/{callId}/answer")
    public ResponseEntity<?> answerCall(@PathVariable("callId") String callId) {
        callService.answerCall(callId);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{callId}/end")
    public ResponseEntity<?> endCall(@PathVariable("callId") String callId) {
        callService.endCall(callId);
        return ResponseEntity.accepted().build();
    }

}
