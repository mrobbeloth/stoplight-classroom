package com.stoplight.classroom.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "session_snapshots")
public class SessionSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(nullable = false)
    private long greenCount;

    @Column(nullable = false)
    private long yellowCount;

    @Column(nullable = false)
    private long redCount;

    @Column(nullable = false)
    private long studentCount;

    @Column(nullable = false, updatable = false)
    private Instant capturedAt;

    @PrePersist
    void onCreate() { capturedAt = Instant.now(); }

    public SessionSnapshot() {}

    public SessionSnapshot(Session session, long greenCount, long yellowCount, long redCount, long studentCount) {
        this.session = session;
        this.greenCount = greenCount;
        this.yellowCount = yellowCount;
        this.redCount = redCount;
        this.studentCount = studentCount;
    }

    public Long getId() { return id; }
    public Session getSession() { return session; }
    public long getGreenCount() { return greenCount; }
    public long getYellowCount() { return yellowCount; }
    public long getRedCount() { return redCount; }
    public long getStudentCount() { return studentCount; }
    public Instant getCapturedAt() { return capturedAt; }
}
