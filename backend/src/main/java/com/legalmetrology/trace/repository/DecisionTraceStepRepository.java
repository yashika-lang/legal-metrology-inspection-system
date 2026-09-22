package com.legalmetrology.trace.repository;

import com.legalmetrology.trace.entity.DecisionTraceStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DecisionTraceStepRepository extends JpaRepository<DecisionTraceStep, UUID> {

    List<DecisionTraceStep> findByInspectionIdOrderByCreatedAtAsc(UUID inspectionId);
}
