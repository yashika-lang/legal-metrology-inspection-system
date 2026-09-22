import { Sparkles, ScanText, GitMerge, Barcode } from "lucide-react";
import { cn } from "@/lib/cn";

/** Shows which evidence source actually contributed a fused declaration's value — real provenance, not a generic "AI" label. A known product's verified master data (barcode-first workflow) always outranks OCR/Vision AI when present. */
export function SourceBadge({
  ocrValue,
  visionValue,
  fromProductDatabase,
}: {
  ocrValue: string | null;
  visionValue: string | null;
  fromProductDatabase?: boolean;
}) {
  const hasOcr = ocrValue != null;
  const hasVision = visionValue != null;

  if (fromProductDatabase) {
    return (
      <span className="inline-flex items-center gap-1 rounded-full bg-success-soft px-1.5 py-0.5 text-[10px] font-medium text-success-foreground">
        <Barcode className="size-2.5" /> Product Database
      </span>
    );
  }
  if (hasOcr && hasVision) {
    return (
      <span className="inline-flex items-center gap-1 rounded-full bg-accent-soft px-1.5 py-0.5 text-[10px] font-medium text-accent-soft-foreground">
        <GitMerge className="size-2.5" /> OCR + Vision AI
      </span>
    );
  }
  if (hasVision) {
    return (
      <span className={cn("inline-flex items-center gap-1 rounded-full px-1.5 py-0.5 text-[10px] font-medium", "bg-ai-violet/15 text-ai-violet")}>
        <Sparkles className="size-2.5" /> Vision AI
      </span>
    );
  }
  if (hasOcr) {
    return (
      <span className="inline-flex items-center gap-1 rounded-full bg-info-soft px-1.5 py-0.5 text-[10px] font-medium text-info-foreground">
        <ScanText className="size-2.5" /> OCR
      </span>
    );
  }
  return null;
}
