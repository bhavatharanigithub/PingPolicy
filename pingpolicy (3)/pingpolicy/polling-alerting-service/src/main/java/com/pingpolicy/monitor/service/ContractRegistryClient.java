package com.pingpolicy.monitor.service;

import com.pingpolicy.monitor.model.RegisteredContract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class ContractRegistryClient {

    private final RestTemplate restTemplate;
    private final String registryBaseUrl;

    public ContractRegistryClient(RestTemplate restTemplate,
                                   @Value("${pingpolicy.registry-url}") String registryBaseUrl) {
        this.restTemplate = restTemplate;
        this.registryBaseUrl = registryBaseUrl;
    }

    public List<RegisteredContract> fetchActiveContracts() {
        RegisteredContract[] contracts =
                restTemplate.getForObject(registryBaseUrl + "/api/contracts/active", RegisteredContract[].class);
        return contracts == null ? List.of() : Arrays.asList(contracts);
    }
}
