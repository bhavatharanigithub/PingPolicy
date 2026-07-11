package com.pingpolicy.monitor.repository;

import com.pingpolicy.monitor.model.DriftEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DriftEventRepository extends JpaRepository<DriftEvent, String> {

    List<DriftEvent> findByContractIdOrderByDetectedAtDesc(String contractId);

    List<DriftEvent> findTop50ByOrderByDetectedAtDesc();
}
