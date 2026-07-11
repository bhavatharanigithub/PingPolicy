package com.pingpolicy.registry.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A registered API contract: the expected JSON shape for a given endpoint,
 * plus the polling configuration the polling-alerting-service should use.
 */
@Entity
@Table(name = "api_contracts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiContract {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String serviceName;

    @Column(nullable = false)
    private String endpointUrl;

    @Column(nullable = false)
    private String httpMethod;

    /** JSON-schema-ish representation of the expected response shape. */
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String expectedSchemaJson;

    /** How often (seconds) the polling service should hit this endpoint. */
    @Column(nullable = false)
    @Builder.Default
    private Integer pollIntervalSeconds = 60;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
