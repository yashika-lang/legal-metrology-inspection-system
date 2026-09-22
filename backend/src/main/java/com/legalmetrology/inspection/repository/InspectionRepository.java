package com.legalmetrology.inspection.repository;

import com.legalmetrology.common.enums.InspectionStatus;
import com.legalmetrology.inspection.entity.Inspection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InspectionRepository extends JpaRepository<Inspection, UUID> {

    /**
     * {@code @EntityGraph} on every list method here: {@code InspectionMapper}
     * reads {@code inspection.getInspector().getFullName()} and
     * {@code inspection.getProduct().getName()} for every row, and this is
     * the highest-traffic list endpoint in the system — without it, a
     * 20-row page costs up to 40 extra lazy-load queries.
     */
    @EntityGraph(attributePaths = {"inspector", "product"})
    Page<Inspection> findByInspectorId(UUID inspectorId, Pageable pageable);

    @EntityGraph(attributePaths = {"inspector", "product"})
    Page<Inspection> findByStatus(InspectionStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"inspector", "product"})
    Page<Inspection> findAll(Pageable pageable);

    /**
     * The "real work only" view: an inspection created via {@code POST
     * /inspections} but abandoned before the AI Pipeline ever ran (no
     * images, no evaluation) has {@code compliance_score IS NULL} forever —
     * that's the same signal the KPI snapshot's {@code AVG(compliance_score)}
     * already uses to ignore them. Reports and the Dashboard's "Recent
     * Inspections" list use these instead of {@link #findAll}/
     * {@link #findByInspectorId} so an inspector re-visiting "New
     * Inspection" without completing anything doesn't clutter either
     * screen with an unnamed, unscored draft.
     */
    @EntityGraph(attributePaths = {"inspector", "product"})
    Page<Inspection> findByComplianceScoreIsNotNull(Pageable pageable);

    @EntityGraph(attributePaths = {"inspector", "product"})
    Page<Inspection> findByInspectorIdAndComplianceScoreIsNotNull(UUID inspectorId, Pageable pageable);
}
