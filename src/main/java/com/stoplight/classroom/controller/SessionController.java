package com.stoplight.classroom.controller;

import com.stoplight.classroom.dto.*;
import com.stoplight.classroom.service.SessionService;
import com.stoplight.classroom.service.StatsService;
import com.stoplight.classroom.service.StoplightService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;
    private final StoplightService stoplightService;
    private final StatsService statsService;
    private final SimpMessagingTemplate messagingTemplate;

    public SessionController(SessionService sessionService, StoplightService stoplightService,
                             StatsService statsService, SimpMessagingTemplate messagingTemplate) {
        this.sessionService = sessionService;
        this.stoplightService = stoplightService;
        this.statsService = statsService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping
    public ResponseEntity<SessionResponse> start(Authentication auth,
                                                  @Valid @RequestBody StartSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sessionService.startSession(auth.getName(), request));
    }

    @PutMapping("/{id}/end")
    public ResponseEntity<SessionResponse> end(Authentication auth, @PathVariable Long id) {
        SessionResponse response = sessionService.endSession(auth.getName(), id);
        statsService.captureSnapshotById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/activity-mode")
    public ResponseEntity<SessionResponse> setActivityMode(Authentication auth, @PathVariable Long id,
                                                            @Valid @RequestBody ActivityModeRequest request) {
        SessionResponse response = sessionService.setActivityMode(auth.getName(), id, request.activityMode());
        messagingTemplate.convertAndSend("/topic/session/" + id + "/activity-mode", request.activityMode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/join")
    public ResponseEntity<JoinSessionResponse> join(@Valid @RequestBody JoinSessionRequest request,
                                                     Authentication auth) {
        Long accountId = null;
        if (auth != null && auth.getName() != null && auth.getName().startsWith("sacct:")) {
            accountId = Long.parseLong(auth.getName().substring("sacct:".length()));
        }
        return ResponseEntity.ok(sessionService.joinSession(request, accountId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.getSession(id));
    }

    @GetMapping("/{id}/aggregate")
    public ResponseEntity<StoplightAggregate> aggregate(@PathVariable Long id) {
        return ResponseEntity.ok(stoplightService.getAggregate(id));
    }
}
