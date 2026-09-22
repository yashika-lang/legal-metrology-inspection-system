package com.legalmetrology.ocr.repository;

import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.ocr.entity.FusedDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FusedDeclarationRepository extends JpaRepository<FusedDeclaration, UUID> {

    List<FusedDeclaration> findByInspectionId(UUID inspectionId);

    Optional<FusedDeclaration> findByInspectionIdAndDeclarationType(UUID inspectionId, DeclarationType declarationType);

    /** Bulk form of the above, for building a display-name fallback across a whole page of inspections without N+1 queries. */
    List<FusedDeclaration> findByInspectionIdInAndDeclarationType(List<UUID> inspectionIds, DeclarationType declarationType);
}
