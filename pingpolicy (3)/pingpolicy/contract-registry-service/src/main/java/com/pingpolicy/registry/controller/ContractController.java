package com.pingpolicy.registry.controller;

import com.pingpolicy.registry.dto.ContractRequest;
import com.pingpolicy.registry.model.ApiContract;
import com.pingpolicy.registry.repository.ApiContractRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
@CrossOrigin(origins = "*")
public class ContractController {

    private final ApiContractRepository repository;

    public ContractController(ApiContractRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ApiContract> listAll() {
        return repository.findAll();
    }

    @GetMapping("/active")
    public List<ApiContract> listActive() {
        return repository.findByActiveTrue();
    }

    @GetMapping("/{id}")
    public ApiContract getOne(@PathVariable String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract not found: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiContract create(@Valid @RequestBody ContractRequest request) {
        ApiContract contract = ApiContract.builder()
                .serviceName(request.getServiceName())
                .endpointUrl(request.getEndpointUrl())
                .httpMethod(request.getHttpMethod().toUpperCase())
                .expectedSchemaJson(request.getExpectedSchemaJson())
                .pollIntervalSeconds(request.getPollIntervalSeconds())
                .active(request.getActive() == null || request.getActive())
                .build();
        return repository.save(contract);
    }

    @PutMapping("/{id}")
    public ApiContract update(@PathVariable String id, @Valid @RequestBody ContractRequest request) {
        ApiContract existing = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract not found: " + id));

        existing.setServiceName(request.getServiceName());
        existing.setEndpointUrl(request.getEndpointUrl());
        existing.setHttpMethod(request.getHttpMethod().toUpperCase());
        existing.setExpectedSchemaJson(request.getExpectedSchemaJson());
        existing.setPollIntervalSeconds(request.getPollIntervalSeconds());
        existing.setActive(request.getActive() == null || request.getActive());

        return repository.save(existing);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract not found: " + id);
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
