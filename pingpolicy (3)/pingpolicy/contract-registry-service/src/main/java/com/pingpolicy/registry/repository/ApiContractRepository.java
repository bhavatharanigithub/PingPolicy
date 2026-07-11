package com.pingpolicy.registry.repository;

import com.pingpolicy.registry.model.ApiContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiContractRepository extends JpaRepository<ApiContract, String> {

    Optional<ApiContract> findByServiceName(String serviceName);

    List<ApiContract> findByActiveTrue();
}
