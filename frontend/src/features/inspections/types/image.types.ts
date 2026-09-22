import type { ImageRecommendedAction, ImageType } from "@/types/enums";

/** Mirrors `com.legalmetrology.inspection.dto.ImageResponse`. */
export interface ImageResponse {
  id: string;
  inspectionId: string;
  storagePath: string;
  signedUrl: string;
  imageType: ImageType;
  qualityScore: number | null;
  qualityWarnings: string[];
  recommendedAction: ImageRecommendedAction | null;
  uploadedAt: string;
}
