package com.stoplight.classroom.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "student_participants")
public class StudentParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(nullable = false)
    private String displayName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_account_id")
    private StudentAccount studentAccount;

    @Column(nullable = false, updatable = false)
    private Instant joinedAt;

    @PrePersist
    void onCreate() { joinedAt = Instant.now(); }

    public StudentParticipant() {}

    public StudentParticipant(Session session, String displayName) {
        this.session = session;
        this.displayName = displayName;
    }

    public Long getId() { return id; }
    public Session getSession() { return session; }
    public String getDisplayName() { return displayName; }
    public StudentAccount getStudentAccount() { return studentAccount; }
    public void setStudentAccount(StudentAccount studentAccount) { this.studentAccount = studentAccount; }
    public Instant getJoinedAt() { return joinedAt; }
}
