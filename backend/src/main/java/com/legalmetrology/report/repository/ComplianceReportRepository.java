package com.legalmetrology.report.repository;

import com.legalmetrology.report.entity.ComplianceReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComplianceReportRepository extends JpaRepository<ComplianceReport, UUID> {

    List<ComplianceReport> findByInspectionId(UUID inspectionId);

    boolean existsByReportNumber(String reportNumber);
}
