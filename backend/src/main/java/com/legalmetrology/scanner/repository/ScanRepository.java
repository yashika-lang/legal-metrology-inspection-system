package com.legalmetrology.scanner.repository;

import com.legalmetrology.scanner.entity.Scan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScanRepository extends JpaRepository<Scan, UUID> {

    List<Scan> findByInspectionId(UUID inspectionId);
}
