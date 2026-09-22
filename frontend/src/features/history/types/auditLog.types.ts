/** Mirrors `com.legalmetrology.history.dto.AuditLogResponse`. */
export interface AuditLogResponse {
  id: string;
  userId: string;
  userName: string;
  action: string;
  entityType: string;
  entityId: string;
  metadata: string;
  ipAddress: string;
  createdAt: string;
}
