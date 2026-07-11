package com.pingpolicy.monitor.controller;

import com.pingpolicy.monitor.model.DriftEvent;
import com.pingpolicy.monitor.repository.DriftEventRepository;
import com.pingpolicy.monitor.service.PollingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DriftEventController {

    private final DriftEventRepository driftEventRepository;
    private final PollingService pollingService;

    public DriftEventController(DriftEventRepository driftEventRepository, PollingService pollingService) {
        this.driftEventRepository = driftEventRepository;
        this.pollingService = pollingService;
    }

    @GetMapping("/drift-events")
    public List<DriftEvent> recentEvents() {
        return driftEventRepository.findTop50ByOrderByDetectedAtDesc();
    }

    @GetMapping("/drift-events/contract/{contractId}")
    public List<DriftEvent> eventsForContract(@PathVariable String contractId) {
        return driftEventRepository.findByContractIdOrderByDetectedAtDesc(contractId);
    }

    @GetMapping("/drift-events/{id}")
    public DriftEvent getOne(@PathVariable String id) {
        return driftEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Drift event not found: " + id));
    }

    @GetMapping("/stats")
    public PollingService.PollStats stats() {
        return pollingService.getStats();
    }
}
