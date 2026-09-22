export type ScanType = "BARCODE" | "QR" | "MANUAL";

/** Mirrors `com.legalmetrology.scanner.dto.ScanResponse`. */
export interface ScanResponse {
  id: string;
  inspectionId: string;
  scanType: ScanType;
  scanValue: string;
  scannedAt: string;
  matchedProductId: string | null;
  matchedProductName: string | null;
}
