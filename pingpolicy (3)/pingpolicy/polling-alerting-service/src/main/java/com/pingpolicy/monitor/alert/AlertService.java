package com.pingpolicy.monitor.alert;

import com.pingpolicy.monitor.model.DriftEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Fires alerts for detected drift events. Supports a generic outbound
 * webhook (Slack-compatible payload shape) plus a structured log sink.
 * Additional sinks (email, PagerDuty, etc.) can be added by implementing
 * the same {@code send} contract.
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final RestTemplate restTemplate;
    private final String webhookUrl;

    public AlertService(RestTemplate restTemplate,
                         @Value("${pingpolicy.alert-webhook-url:}") String webhookUrl) {
        this.restTemplate = restTemplate;
        this.webhookUrl = webhookUrl;
    }

    public void sendDriftAlert(DriftEvent event) {
        String message = String.format(
                "[PingPolicy] %s drift detected on %s (%s) — %d field change(s), severity=%s",
                event.getSeverity(), event.getServiceName(), event.getEndpointUrl(),
                event.getChangeCount(), event.getSeverity());

        log.warn(message);

        if (webhookUrl != null && !webhookUrl.isBlank()) {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("text", message);
                payload.put("serviceName", event.getServiceName());
                payload.put("endpointUrl", event.getEndpointUrl());
                payload.put("severity", event.getSeverity().name());
                payload.put("changeCount", event.getChangeCount());
                payload.put("diffSummary", event.getDiffSummaryJson());

                restTemplate.postForLocation(webhookUrl, payload);
            } catch (Exception e) {
                log.error("Failed to deliver drift alert webhook for contract {}: {}",
                        event.getContractId(), e.getMessage());
            }
        }
    }
}
