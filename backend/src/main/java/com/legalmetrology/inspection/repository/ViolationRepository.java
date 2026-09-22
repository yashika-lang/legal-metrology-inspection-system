package com.legalmetrology.inspection.repository;

import com.legalmetrology.inspection.entity.Violation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface ViolationRepository extends JpaRepository<Violation, UUID> {

    /**
     * Fetch-joins {@code rule} because every caller (the Copilot context
     * assembler, {@code ViolationMapper}, the evidence engine) immediately
     * reads {@code violation.getRule()} for every row — without the join
     * that's an N+1 lazy-load per violation. Same signature/behavior as a
     * derived query, just without the extra round trips.
     */
    @Query("select v from Violation v join fetch v.rule where v.inspection.id = :inspectionId")
    List<Violation> findByInspectionId(@Param("inspectionId") UUID inspectionId);

    /** Violations are the current rule-engine outcome, not an audit log — re-evaluation replaces them rather than accumulating duplicates. */
    @Transactional
    void deleteByInspectionId(UUID inspectionId);
}
