package com.stoplight.classroom.controller;

import com.stoplight.classroom.dto.StoplightAggregate;
import com.stoplight.classroom.dto.StoplightRequest;
import com.stoplight.classroom.service.StoplightService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stoplight")
public class StoplightController {

    private final StoplightService stoplightService;
    private final SimpMessagingTemplate messagingTemplate;

    public StoplightController(StoplightService stoplightService,
                               SimpMessagingTemplate messagingTemplate) {
        this.stoplightService = stoplightService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/{sessionId}")
    public ResponseEntity<Void> submit(Authentication auth, @PathVariable Long sessionId,
                                        @Valid @RequestBody StoplightRequest request) {
        String principal = auth.getName(); // "student:<participantId>"
        if (!principal.startsWith("student:")) {
            throw new IllegalArgumentException("Only students can submit responses");
        }
        Long participantId = Long.parseLong(principal.substring("student:".length()));
        stoplightService.submitResponse(participantId, sessionId, request);

        StoplightAggregate aggregate = stoplightService.getAggregate(sessionId);
        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/aggregate", aggregate);

        return ResponseEntity.ok().build();
    }
}
