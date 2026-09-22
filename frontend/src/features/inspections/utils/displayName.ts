import type { InspectionResponse } from "../types";

/**
 * Every list that shows an inspection (Reports, Dashboard, AI Copilot) used
 * to fall back to the literal string "Untitled Inspection" whenever
 * `productName` was null — which was nearly always, because nothing ever
 * actually set it: a barcode scan match was shown in the UI but never
 * persisted onto the inspection, and there was no fallback at all for the
 * (very common) case of a photo-only inspection with no scan. The backend
 * now fills `productName` from a real linked product OR the OCR/Vision-fused
 * PRODUCT_NAME declaration once the pipeline has run — this only covers the
 * remaining case where genuinely nothing is known yet (pipeline hasn't run,
 * no scan). "Untitled Inspection" reads like a bug; the region and start
 * date are real, always-available fields, so use those instead.
 */
export function inspectionDisplayName(
  inspection: Pick<InspectionResponse, "productName" | "region" | "startedAt">,
): string {
  if (inspection.productName) return inspection.productName;
  const date = new Date(inspection.startedAt).toLocaleDateString(undefined, { day: "numeric", month: "short" });
  return inspection.region ? `${inspection.region} inspection · ${date}` : `Inspection · ${date}`;
}
