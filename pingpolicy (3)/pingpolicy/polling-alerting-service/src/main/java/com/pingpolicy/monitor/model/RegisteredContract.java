package com.pingpolicy.monitor.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Local read-only view of a contract, fetched from contract-registry-service.
 * Deliberately decoupled from that service's entity so the two services can
 * evolve independently.
 */
@Getter
@Setter
@NoArgsConstructor
public class RegisteredContract {
    private String id;
    private String serviceName;
    private String endpointUrl;
    private String httpMethod;
    private String expectedSchemaJson;
    private Integer pollIntervalSeconds;
    private Boolean active;
}
