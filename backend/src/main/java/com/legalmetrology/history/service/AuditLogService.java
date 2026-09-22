package com.legalmetrology.history.service;

import com.legalmetrology.history.dto.AuditLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AuditLogService {

    void record(UUID userId, String action, String entityType, UUID entityId, String metadataJson, String ipAddress);

    Page<AuditLogResponse> listByEntity(String entityType, UUID entityId, Pageable pageable);

    Page<AuditLogResponse> listByUser(UUID userId, Pageable pageable);

    Page<AuditLogResponse> listAll(Pageable pageable);
}
