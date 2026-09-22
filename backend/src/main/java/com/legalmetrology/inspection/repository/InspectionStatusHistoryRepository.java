package com.legalmetrology.inspection.repository;

import com.legalmetrology.inspection.entity.InspectionStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InspectionStatusHistoryRepository extends JpaRepository<InspectionStatusHistory, UUID> {

    List<InspectionStatusHistory> findByInspectionIdOrderByCreatedAtAsc(UUID inspectionId);
}
