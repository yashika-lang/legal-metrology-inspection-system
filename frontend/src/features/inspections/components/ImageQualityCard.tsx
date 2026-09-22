import { motion } from "framer-motion";
import { CheckCircle2, AlertTriangle, RotateCcw, Sparkles as SparklesIcon } from "lucide-react";
import { QUALITY_WARNING_LABEL } from "@/types/enums";
import { cn } from "@/lib/cn";
import type { ImageResponse } from "../types";

function scoreColor(score: number | null) {
  if (score === null) return "var(--color-faint-foreground)";
  if (score >= 80) return "var(--color-success)";
  if (score >= 50) return "var(--color-warning)";
  return "var(--color-critical)";
}

/**
 * Surfaces the Image Quality Analyzer's real output (`qualityScore`,
 * `qualityWarnings`, `recommendedAction`) that already comes back on
 * every image-upload response — no separate endpoint needed, this data
 * was simply being discarded after upload beyond a tiny thumbnail icon.
 */
export function ImageQualityCard({ image }: { image: ImageResponse | null }) {
  if (!image || image.qualityScore === null) return null;

  const color = scoreColor(image.qualityScore);
  const hasWarnings = image.qualityWarnings.length > 0;

  return (
    <motion.div
      initial={{ opacity: 0, y: 6 }}
      animate={{ opacity: 1, y: 0 }}
      className="card-hover rounded-lg border border-border bg-surface-raised p-3"
    >
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-1.5">
          <SparklesIcon className="size-3.5 text-ai-violet" />
          <p className="text-xs font-semibold text-foreground">Image Quality</p>
        </div>
        <span className="tabular text-sm font-semibold" style={{ color }}>
          {Math.round(image.qualityScore)}
          <span className="text-[10px] font-normal text-faint-foreground">/100</span>
        </span>
      </div>

      <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-surface-sunken">
        <motion.div
          className="h-full rounded-full"
          style={{ backgroundColor: color }}
          initial={{ width: 0 }}
          animate={{ width: `${image.qualityScore}%` }}
          transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
        />
      </div>

      {hasWarnings ? (
        <ul className="mt-2.5 space-y-1">
          {image.qualityWarnings.map((warning) => (
            <li key={warning} className="flex items-center gap-1.5 text-[11px] text-foreground-secondary">
              <AlertTriangle className="size-3 shrink-0 text-warning" />
              {QUALITY_WARNING_LABEL[warning] ?? warning}
            </li>
          ))}
        </ul>
      ) : (
        <p className="mt-2.5 flex items-center gap-1.5 text-[11px] text-success-foreground">
          <CheckCircle2 className="size-3" /> No quality issues detected
        </p>
      )}

      {image.recommendedAction && image.recommendedAction !== "NONE" && (
        <div
          className={cn(
            "mt-2.5 flex items-center gap-1.5 rounded-md px-2 py-1.5 text-[11px] font-medium",
            image.recommendedAction === "RETAKE" ? "bg-critical-soft text-critical-foreground" : "bg-warning-soft text-warning-foreground",
          )}
        >
          <RotateCcw className="size-3" />
          {image.recommendedAction === "RETAKE" ? "Recommended: retake this photo" : "Recommended: enhance before analysis"}
        </div>
      )}
    </motion.div>
  );
}
