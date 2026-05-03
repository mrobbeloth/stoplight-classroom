package com.stoplight.classroom.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sessions")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, unique = true)
    private String joinCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityMode activityMode = ActivityMode.SILENT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private Instant startedAt;

    private Instant endedAt;

    @PrePersist
    void onCreate() { startedAt = Instant.now(); }

    public Session() {}

    public Session(Course course, String joinCode) {
        this.course = course;
        this.joinCode = joinCode;
    }

    public Long getId() { return id; }
    public Course getCourse() { return course; }
    public String getJoinCode() { return joinCode; }
    public ActivityMode getActivityMode() { return activityMode; }
    public void setActivityMode(ActivityMode activityMode) { this.activityMode = activityMode; }
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
}
