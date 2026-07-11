package com.pingpolicy.monitor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A persisted record of a detected schema drift event: which contract
 * drifted, what changed, and when.
 */
@Entity
@Table(name = "drift_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriftEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String contractId;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private String endpointUrl;

    /** JSON array of individual field-level diffs (see JsonDiffEngine). */
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String diffSummaryJson;

    @Column(nullable = false)
    private Integer changeCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Column(nullable = false)
    private Boolean alertSent;

    @Column(nullable = false, updatable = false)
    private Instant detectedAt;

    @PrePersist
    void onCreate() {
        this.detectedAt = Instant.now();
    }

    public enum Severity {
        LOW,      // additive change, e.g. new optional field
        MEDIUM,   // type change on a non-critical field
        HIGH      // field removed / required field type change
    }
}
