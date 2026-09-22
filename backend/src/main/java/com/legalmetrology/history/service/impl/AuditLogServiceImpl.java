package com.legalmetrology.history.service.impl;

import com.legalmetrology.auth.repository.UserRepository;
import com.legalmetrology.history.dto.AuditLogResponse;
import com.legalmetrology.history.entity.AuditLog;
import com.legalmetrology.history.mapper.AuditLogMapper;
import com.legalmetrology.history.repository.AuditLogRepository;
import com.legalmetrology.history.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final AuditLogMapper auditLogMapper;

    @Override
    @Transactional
    public void record(UUID userId, String action, String entityType, UUID entityId, String metadataJson, String ipAddress) {
        AuditLog auditLog = AuditLog.builder()
                .user(userId != null ? userRepository.findById(userId).orElse(null) : null)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .metadata(metadataJson)
                .ipAddress(ipAddress)
                .build();
        auditLogRepository.save(auditLog);
        log.debug("Audit log recorded: action={} entityType={} entityId={}", action, entityType, entityId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> listByEntity(String entityType, UUID entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable).map(auditLogMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> listByUser(UUID userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable).map(auditLogMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> listAll(Pageable pageable) {
        return auditLogRepository.findAll(pageable).map(auditLogMapper::toResponse);
    }
}
