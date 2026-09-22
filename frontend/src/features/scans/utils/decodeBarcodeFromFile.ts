import { BrowserMultiFormatReader } from "@zxing/browser";

/**
 * Best-effort barcode/QR read directly from a photo file — not just the
 * live scanner feed. A Guided Capture "Back" photo very often has the
 * barcode plainly visible in frame; there's no reason an inspector should
 * have to separately open the scanner and re-point the camera at the same
 * physical barcode they just photographed. Returns null on anything that
 * isn't a clean read (wrong angle, too small, genuinely no barcode in
 * frame) — this is silent best-effort, never a user-facing error.
 */
export async function decodeBarcodeFromFile(file: File): Promise<string | null> {
  const url = URL.createObjectURL(file);
  try {
    const reader = new BrowserMultiFormatReader();
    const result = await reader.decodeFromImageUrl(url);
    return result.getText();
  } catch {
    return null;
  } finally {
    URL.revokeObjectURL(url);
  }
}
