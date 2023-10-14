package az.risk.agentx.controller;


import az.risk.agentx.dto.ChangeStateDto;
import az.risk.agentx.model.Response;
import az.risk.agentx.service.SubscriptionService;
import az.risk.agentx.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

import static org.springframework.http.HttpStatus.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("user")
public class UserController {

    private final SubscriptionService subscriptionService;
    private final UserService userService;


    @PostMapping("connect")
    public ResponseEntity<?> connect(@RequestParam("extension") int extension) {


        return ResponseEntity.ok(
                Response.builder()
                        .timeStamp(LocalDateTime.now())
                        .status(OK.value())
                        .message("Agent Connected")
                        .data(subscriptionService.subscribe(extension))
                        .build());
    }

    @DeleteMapping("disconnect")
    public ResponseEntity<?> disconnect(@RequestParam("reasonCodeId") String reasonCodeId) {
        subscriptionService.unsubscribe(reasonCodeId);
        return ResponseEntity.ok(
                Response.builder()
                        .timeStamp(LocalDateTime.now())
                        .status(OK.value())
                        .message("Agent Disconnected!")
                        .build());

    }

    @PutMapping("state")
    public ResponseEntity<?> changeState(@RequestBody ChangeStateDto changeStateDto) {
        return ResponseEntity.ok(
                Response.builder()
                        .timeStamp(LocalDateTime.now())
                        .status(OK.value())
                        .message("State changed!")
                        .data(userService.changeState(changeStateDto))
                        .build());
    }

    @GetMapping("reason-codes")
    public ResponseEntity<?> getReasonCodeList(@RequestParam("category") String category){
        return ResponseEntity.ok(
                Response.builder()
                        .timeStamp(LocalDateTime.now())
                        .status(OK.value())
                        .data(userService.getReasonCodeListByCategory(category))
                        .message("Reason codes retrieved successfully")
                        .build());
    }

}
