import type { FraudRisk, InspectionStatus } from "@/types/enums";

/** Mirrors `com.legalmetrology.inspection.dto.InspectionResponse` field-for-field. */
export interface InspectionResponse {
  id: string;
  inspectorId: string;
  inspectorName: string;
  productId: string | null;
  productName: string | null;
  status: InspectionStatus;
  locationLat: number | null;
  locationLng: number | null;
  region: string | null;
  complianceScore: number | null;
  fraudRisk: FraudRisk | null;
  startedAt: string;
  completedAt: string | null;
}

/** Mirrors `com.legalmetrology.inspection.dto.InspectionRequest`. */
export interface InspectionRequest {
  productId?: string | null;
  locationLat?: number | null;
  locationLng?: number | null;
  region?: string | null;
}

/** Mirrors `com.legalmetrology.inspection.dto.InspectionStatusHistoryResponse`. */
export interface InspectionStatusHistoryResponse {
  id: string;
  fromStatus: InspectionStatus | null;
  toStatus: InspectionStatus;
  changedByName: string;
  note: string | null;
  createdAt: string;
}
