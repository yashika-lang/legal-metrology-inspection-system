import { Check, X, Minus, Loader2 } from "lucide-react";
import { motion } from "framer-motion";
import { PIPELINE_ORDER, TRACE_STEP_LABEL, type TraceStepName } from "@/types/enums";
import type { DecisionTraceStepResponse } from "../types";
import { cn } from "@/lib/cn";
import type { PipelinePhase } from "../hooks/usePipeline";

const PHASE_TO_STEP: Partial<Record<PipelinePhase, TraceStepName>> = {
  ocr: "OCR",
  vision: "VISION_DETECTION",
  fusion: "DECLARATION_FUSION",
  evaluation: "RULE_EVALUATION",
  evidence: "EVIDENCE_GENERATED",
};

interface DecisionTimelineProps {
  steps: DecisionTraceStepResponse[];
  currentPhase: PipelinePhase;
}

export function DecisionTimeline({ steps, currentPhase }: DecisionTimelineProps) {
  const stepByName = new Map(steps.map((s) => [s.stepName, s]));
  const activeStepName = PHASE_TO_STEP[currentPhase];
  const completedCount = PIPELINE_ORDER.filter((s) => stepByName.has(s)).length;
  const progressPct = Math.round((completedCount / PIPELINE_ORDER.length) * 100);

  return (
    <div className="flex h-full items-center gap-3 px-4 py-2.5">
      <div className="hidden shrink-0 items-center gap-1.5 sm:flex">
        <span className="tabular text-[11px] font-semibold text-foreground">{progressPct}%</span>
        <div className="h-1 w-12 overflow-hidden rounded-full bg-surface-sunken">
          <motion.div
            className="bg-gradient-ai h-full rounded-full"
            initial={false}
            animate={{ width: `${progressPct}%` }}
            transition={{ duration: 0.4 }}
          />
        </div>
      </div>
      <div className="flex min-w-0 flex-1 items-center gap-1 overflow-x-auto">
      {PIPELINE_ORDER.map((stepName, index) => {
        const recorded = stepByName.get(stepName);
        const isActive = stepName === activeStepName;
        const isPast = recorded !== undefined;

        return (
          <div key={stepName} className="flex shrink-0 items-center gap-1">
            {index > 0 && (
              <div
                className={cn(
                  "h-px w-4",
                  isPast || isActive ? "bg-accent/50" : "bg-border",
                )}
              />
            )}
            <motion.div
              initial={false}
              animate={isActive ? { scale: [1, 1.06, 1] } : { scale: 1 }}
              transition={{ repeat: isActive ? Infinity : 0, duration: 1.4 }}
              className={cn(
                "flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[11px] font-medium transition-colors",
                recorded?.status === "SUCCESS" && "border-success/30 bg-success-soft text-success-foreground",
                recorded?.status === "FAILED" && "border-critical/30 bg-critical-soft text-critical-foreground",
                recorded?.status === "SKIPPED" && "border-border bg-surface-sunken text-muted-foreground",
                !recorded && isActive && "border-accent/40 bg-accent-soft text-accent-soft-foreground",
                !recorded && !isActive && "border-border text-faint-foreground",
              )}
              title={recorded?.outputSummary ?? recorded?.reason ?? undefined}
            >
              {isActive && !recorded && <Loader2 className="size-3 animate-spin" />}
              {recorded?.status === "SUCCESS" && <Check className="size-3" />}
              {recorded?.status === "FAILED" && <X className="size-3" />}
              {recorded?.status === "SKIPPED" && <Minus className="size-3" />}
              {TRACE_STEP_LABEL[stepName]}
              {recorded?.confidence != null && (
                <span className="tabular opacity-70">{Math.round(recorded.confidence * 100)}%</span>
              )}
            </motion.div>
          </div>
        );
      })}
      </div>
    </div>
  );
}
