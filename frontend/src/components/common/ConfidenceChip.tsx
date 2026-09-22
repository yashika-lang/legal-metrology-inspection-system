import { cn } from "@/lib/cn";

/** A small, consistent confidence indicator reused everywhere a fused/detection confidence is shown (Fields tab, Violations, Evidence) — color reflects real confidence bands, not decoration. */
export function ConfidenceChip({ confidence }: { confidence: number | null | undefined }) {
  if (confidence == null) return null;
  const pct = Math.round(confidence * 100);
  const tone =
    pct >= 80 ? "bg-success-soft text-success-foreground" : pct >= 50 ? "bg-warning-soft text-warning-foreground" : "bg-critical-soft text-critical-foreground";

  return <span className={cn("tabular inline-flex items-center rounded-full px-1.5 py-0.5 text-[10px] font-medium", tone)}>{pct}%</span>;
}
