package com.legalmetrology.history.mapper;

import com.legalmetrology.history.dto.AuditLogResponse;
import com.legalmetrology.history.entity.AuditLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    default AuditLogResponse toResponse(AuditLog auditLog) {
        if (auditLog == null) {
            return null;
        }
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getUser() != null ? auditLog.getUser().getId() : null,
                auditLog.getUser() != null ? auditLog.getUser().getFullName() : null,
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getMetadata(),
                auditLog.getIpAddress(),
                auditLog.getCreatedAt()
        );
    }
}
