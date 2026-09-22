package com.legalmetrology.evidence.repository;

import com.legalmetrology.evidence.entity.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvidenceRepository extends JpaRepository<Evidence, UUID> {

    @Query("select e from Evidence e join fetch e.violation v join fetch v.rule where e.inspection.id = :inspectionId order by e.createdAt asc")
    List<Evidence> findByInspectionId(@Param("inspectionId") UUID inspectionId);

    @Query("select e from Evidence e join fetch e.violation v join fetch v.rule where e.violation.id = :violationId")
    Optional<Evidence> findByViolationId(@Param("violationId") UUID violationId);
}
