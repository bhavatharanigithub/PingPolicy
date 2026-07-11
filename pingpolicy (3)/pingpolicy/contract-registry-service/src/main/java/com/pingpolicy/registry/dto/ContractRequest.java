package com.pingpolicy.registry.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContractRequest {

    @NotBlank
    private String serviceName;

    @NotBlank
    private String endpointUrl;

    @NotBlank
    private String httpMethod;

    @NotBlank
    private String expectedSchemaJson;

    @Min(5)
    private Integer pollIntervalSeconds = 60;

    private Boolean active = true;
}
