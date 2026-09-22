import type { FusedDeclarationResponse } from "../types";

/**
 * A missing MRP reads very differently from a missing address: MRP is
 * something OCR/Vision AI actively tried and failed to read off the
 * package (or never got the chance to, if the pipeline hasn't run), never
 * a value this system invents — "Unable to Read" says that plainly,
 * instead of a bare "—" that could be misread as "there is no MRP."
 * Every other declaration type keeps the plain dash for "not detected."
 */
export function fusedValueDisplay(d: Pick<FusedDeclarationResponse, "declarationType" | "fusedValue">): string {
  if (d.fusedValue) return d.fusedValue;
  return d.declarationType === "MRP" ? "Unable to Read" : "—";
}
