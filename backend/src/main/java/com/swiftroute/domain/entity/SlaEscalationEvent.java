package com.swiftroute.domain.entity;

import com.swiftroute.domain.enums.EscalationStage;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "sla_escalation_events",
       uniqueConstraints = @UniqueConstraint(name = "uq_job_threshold", columnNames = {"job_id", "threshold_stage"}))
public class SlaEscalationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(name = "threshold_stage", nullable = false, length = 30)
    private EscalationStage thresholdStage;

    @Column(name = "triggered_at", nullable = false, updatable = false)
    private Instant triggeredAt;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    public SlaEscalationEvent() {
    }

    public SlaEscalationEvent(Job job, EscalationStage thresholdStage, String details) {
        this.job = job;
        this.thresholdStage = thresholdStage;
        this.details = details;
    }

    @PrePersist
    protected void onCreate() {
        if (this.triggeredAt == null) {
            this.triggeredAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public EscalationStage getThresholdStage() {
        return thresholdStage;
    }

    public void setThresholdStage(EscalationStage thresholdStage) {
        this.thresholdStage = thresholdStage;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(Instant triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
