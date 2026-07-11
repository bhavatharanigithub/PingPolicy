package com.pingpolicy.monitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingpolicy.monitor.alert.AlertService;
import com.pingpolicy.monitor.diff.JsonDiffEngine;
import com.pingpolicy.monitor.diff.JsonDiffEngine.FieldDiff;
import com.pingpolicy.monitor.model.DriftEvent;
import com.pingpolicy.monitor.model.RegisteredContract;
import com.pingpolicy.monitor.repository.DriftEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class PollingService {

    private static final Logger log = LoggerFactory.getLogger(PollingService.class);

    private final RestTemplate restTemplate;
    private final PollCacheService pollCacheService;
    private final JsonDiffEngine diffEngine;
    private final DriftEventRepository driftEventRepository;
    private final AlertService alertService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // simple in-memory counters exposed for the /api/stats endpoint
    private final AtomicLong totalPolls = new AtomicLong();
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicInteger driftsDetected = new AtomicInteger();

    public PollingService(RestTemplate restTemplate,
                           PollCacheService pollCacheService,
                           JsonDiffEngine diffEngine,
                           DriftEventRepository driftEventRepository,
                           AlertService alertService) {
        this.restTemplate = restTemplate;
        this.pollCacheService = pollCacheService;
        this.diffEngine = diffEngine;
        this.driftEventRepository = driftEventRepository;
        this.alertService = alertService;
    }

    /**
     * Polls one contract's live endpoint and checks it for drift.
     * Returns true if the poll completed (regardless of whether drift was found).
     */
    public boolean pollContract(RegisteredContract contract) {
        totalPolls.incrementAndGet();
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    contract.getEndpointUrl(),
                    HttpMethod.valueOf(contract.getHttpMethod().toUpperCase()),
                    null,
                    String.class);

            String body = response.getBody();
            if (body == null) {
                log.warn("Empty response body from {}", contract.getEndpointUrl());
                return false;
            }

            // --- Redis cache-aside check: skip the diff if nothing changed ---
            if (pollCacheService.isUnchangedSinceLastPoll(contract.getId(), body)) {
                cacheHits.incrementAndGet();
                log.debug("Cache hit for contract {} — response unchanged, skipping diff", contract.getId());
                return true;
            }

            JsonNode expected = objectMapper.readTree(contract.getExpectedSchemaJson());
            JsonNode observed = objectMapper.readTree(body);

            List<FieldDiff> diffs = diffEngine.diff(expected, observed);

            // response changed but shape didn't -> still update the cached hash
            pollCacheService.recordHash(contract.getId(), body);

            if (!diffs.isEmpty()) {
                handleDrift(contract, diffs);
            }

            return true;
        } catch (Exception e) {
            log.error("Poll failed for contract {} ({}): {}", contract.getId(), contract.getEndpointUrl(), e.getMessage());
            return false;
        }
    }

    private void handleDrift(RegisteredContract contract, List<FieldDiff> diffs) throws Exception {
        var classification = diffEngine.classify(diffs);

        DriftEvent.Severity severity = switch (classification.severity()) {
            case HIGH -> DriftEvent.Severity.HIGH;
            case MEDIUM -> DriftEvent.Severity.MEDIUM;
            default -> DriftEvent.Severity.LOW;
        };

        DriftEvent event = DriftEvent.builder()
                .contractId(contract.getId())
                .serviceName(contract.getServiceName())
                .endpointUrl(contract.getEndpointUrl())
                .diffSummaryJson(objectMapper.writeValueAsString(diffs))
                .changeCount(diffs.size())
                .severity(severity)
                .alertSent(false)
                .build();

        driftEventRepository.save(event);
        driftsDetected.incrementAndGet();

        alertService.sendDriftAlert(event);
        event.setAlertSent(true);
        driftEventRepository.save(event);
    }

    public PollStats getStats() {
        return new PollStats(totalPolls.get(), cacheHits.get(), driftsDetected.get());
    }

    public record PollStats(long totalPolls, long cacheHits, int driftsDetected) {
        public double cacheHitRatio() {
            return totalPolls == 0 ? 0.0 : (double) cacheHits / totalPolls;
        }
    }
}
