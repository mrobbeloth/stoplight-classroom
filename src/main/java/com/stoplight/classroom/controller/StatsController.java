package com.stoplight.classroom.controller;

import com.stoplight.classroom.dto.CourseStatsResponse;
import com.stoplight.classroom.dto.LifetimeStatsResponse;
import com.stoplight.classroom.dto.SessionStatsResponse;
import com.stoplight.classroom.service.ExportService;
import com.stoplight.classroom.service.StatsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;
    private final ExportService exportService;

    public StatsController(StatsService statsService, ExportService exportService) {
        this.statsService = statsService;
        this.exportService = exportService;
    }

    @GetMapping("/session/{id}")
    public ResponseEntity<SessionStatsResponse> sessionStats(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(statsService.getSessionStats(auth.getName(), id));
    }

    @GetMapping("/course/{id}")
    public ResponseEntity<CourseStatsResponse> courseStats(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(statsService.getCourseStats(auth.getName(), id));
    }

    @GetMapping("/lifetime")
    public ResponseEntity<LifetimeStatsResponse> lifetimeStats(Authentication auth) {
        return ResponseEntity.ok(statsService.getLifetimeStats(auth.getName()));
    }

    @GetMapping("/session/{id}/csv")
    public ResponseEntity<String> sessionCsv(Authentication auth, @PathVariable Long id) {
        return csvResponse(exportService.exportSessionCsv(auth.getName(), id), "session-" + id + ".csv");
    }

    @GetMapping("/course/{id}/csv")
    public ResponseEntity<String> courseCsv(Authentication auth, @PathVariable Long id) {
        return csvResponse(exportService.exportCourseCsv(auth.getName(), id), "course-" + id + ".csv");
    }

    private ResponseEntity<String> csvResponse(String csv, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
