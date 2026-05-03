package com.stoplight.classroom.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "stoplight_responses",
       uniqueConstraints = @UniqueConstraint(columnNames = {"participant_id", "session_id"}))
public class StoplightResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private StudentParticipant participant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Enumerated(EnumType.STRING)
    @Column(name = "stoplight_value", nullable = false)
    private StoplightValue value;

    @Column(nullable = false)
    private Instant submittedAt;

    @PrePersist
    void onCreate() { submittedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { submittedAt = Instant.now(); }

    public StoplightResponse() {}

    public StoplightResponse(StudentParticipant participant, Session session, StoplightValue value) {
        this.participant = participant;
        this.session = session;
        this.value = value;
    }

    public Long getId() { return id; }
    public StudentParticipant getParticipant() { return participant; }
    public Session getSession() { return session; }
    public StoplightValue getValue() { return value; }
    public void setValue(StoplightValue value) { this.value = value; }
    public Instant getSubmittedAt() { return submittedAt; }
}
