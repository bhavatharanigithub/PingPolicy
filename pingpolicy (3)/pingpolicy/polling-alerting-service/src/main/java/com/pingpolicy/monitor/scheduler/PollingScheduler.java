package com.pingpolicy.monitor.scheduler;

import com.pingpolicy.monitor.model.RegisteredContract;
import com.pingpolicy.monitor.service.ContractRegistryClient;
import com.pingpolicy.monitor.service.PollingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fixed-rate driver loop: every {@code pingpolicy.scheduler.fixed-rate-ms}
 * (default 30s) it asks the contract registry for the current set of active
 * contracts and polls each one on its own thread from a small pool, so one
 * slow/unreachable endpoint doesn't stall the rest of the fleet.
 *
 * Note: each contract also carries its own pollIntervalSeconds, which is
 * intended for per-target throttling; this reference implementation polls
 * every active contract every tick and relies on the Redis cache-aside
 * check in PollingService to make repeated unchanged polls cheap. A
 * straightforward extension is to track lastPolledAt per contract and skip
 * ticks where pollIntervalSeconds hasn't elapsed yet.
 */
@Component
public class PollingScheduler {

    private static final Logger log = LoggerFactory.getLogger(PollingScheduler.class);

    private final ContractRegistryClient registryClient;
    private final PollingService pollingService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(8);

    public PollingScheduler(ContractRegistryClient registryClient, PollingService pollingService) {
        this.registryClient = registryClient;
        this.pollingService = pollingService;
    }

    @Scheduled(fixedRateString = "${pingpolicy.scheduler.fixed-rate-ms:30000}")
    public void runPollCycle() {
        List<RegisteredContract> contracts;
        try {
            contracts = registryClient.fetchActiveContracts();
        } catch (Exception e) {
            log.error("Unable to fetch active contracts from registry: {}", e.getMessage());
            return;
        }

        if (contracts.isEmpty()) {
            log.debug("No active contracts to poll");
            return;
        }

        log.info("Starting poll cycle for {} active contract(s)", contracts.size());
        for (RegisteredContract contract : contracts) {
            executorService.submit(() -> pollingService.pollContract(contract));
        }
    }
}
