package com.stoplight.classroom.service;

import com.stoplight.classroom.dto.CourseStatsResponse;
import com.stoplight.classroom.dto.SessionStatsResponse;
import org.springframework.stereotype.Service;

@Service
public class ExportService {

    private final StatsService statsService;

    public ExportService(StatsService statsService) {
        this.statsService = statsService;
    }

    public String exportSessionCsv(String teacherUsername, Long sessionId) {
        SessionStatsResponse s = statsService.getSessionStats(teacherUsername, sessionId);
        var sb = new StringBuilder();
        sb.append("Session ID,Course,Students,Green,Yellow,Red,Started,Ended\n");
        sb.append(s.sessionId()).append(',')
          .append(csvEscape(s.courseName())).append(',')
          .append(s.studentCount()).append(',')
          .append(s.greenCount()).append(',')
          .append(s.yellowCount()).append(',')
          .append(s.redCount()).append(',')
          .append(s.startedAt()).append(',')
          .append(s.endedAt()).append('\n');
        return sb.toString();
    }

    public String exportCourseCsv(String teacherUsername, Long courseId) {
        CourseStatsResponse c = statsService.getCourseStats(teacherUsername, courseId);
        var sb = new StringBuilder();
        sb.append("Session ID,Date,Students,Green,Yellow,Red\n");
        for (SessionStatsResponse s : c.sessions()) {
            sb.append(s.sessionId()).append(',')
              .append(s.startedAt()).append(',')
              .append(s.studentCount()).append(',')
              .append(s.greenCount()).append(',')
              .append(s.yellowCount()).append(',')
              .append(s.redCount()).append('\n');
        }
        return sb.toString();
    }

    private String csvEscape(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
