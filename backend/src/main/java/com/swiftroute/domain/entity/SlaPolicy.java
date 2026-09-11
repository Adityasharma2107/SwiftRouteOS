package com.swiftroute.domain.entity;

import com.swiftroute.domain.enums.Priority;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sla_policies")
public class SlaPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, unique = true, length = 20)
    private Priority priority;

    @Column(name = "response_deadline_minutes", nullable = false)
    private Integer responseDeadlineMinutes;

    @Column(name = "resolution_deadline_minutes", nullable = false)
    private Integer resolutionDeadlineMinutes;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private Instant createdAt;

    public SlaPolicy() {
    }

    public SlaPolicy(Priority priority, Integer responseDeadlineMinutes, Integer resolutionDeadlineMinutes) {
        this.priority = priority;
        this.responseDeadlineMinutes = responseDeadlineMinutes;
        this.resolutionDeadlineMinutes = resolutionDeadlineMinutes;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Integer getResponseDeadlineMinutes() {
        return responseDeadlineMinutes;
    }

    public void setResponseDeadlineMinutes(Integer responseDeadlineMinutes) {
        this.responseDeadlineMinutes = responseDeadlineMinutes;
    }

    public Integer getResolutionDeadlineMinutes() {
        return resolutionDeadlineMinutes;
    }

    public void setResolutionDeadlineMinutes(Integer resolutionDeadlineMinutes) {
        this.resolutionDeadlineMinutes = resolutionDeadlineMinutes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
