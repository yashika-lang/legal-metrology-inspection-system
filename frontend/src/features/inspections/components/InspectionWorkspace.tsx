import { useMemo, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Play, MapPin, Loader2, ImageIcon, Upload, ScanEye, ListChecks, AlertTriangle, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Sheet } from "@/components/ui/sheet";
import { StatusBadge } from "@/components/common/StatusBadge";
import { EmptyState } from "@/components/common/EmptyState";
import { useInspection } from "../hooks/useInspection";
import { useInspectionImages } from "../hooks/useImages";
import { inspectionDisplayName } from "../utils/displayName";
import { useDecisionTrace, useEvidence, useViolations, useRunPipeline } from "../hooks/usePipeline";
import { ImageCapturePanel } from "./ImageCapturePanel";
import { AnnotatedImageViewer } from "@/features/ai-vision/components/AnnotatedImageViewer";
import type { OverlayBox } from "@/features/ai-vision/components/BoundingBoxOverlay";
import { InspectionResultsPanel } from "./InspectionResultsPanel";
import { DecisionTimeline } from "./DecisionTimeline";
import { ChatPanel } from "@/features/ai-assistant/components/ChatPanel";
import { NirikshakFab } from "@/features/ai-assistant/components/NirikshakFab";
import { MINIMUM_REQUIRED_ANGLES } from "./GuidedCaptureGrid";
import { cn } from "@/lib/cn";

export function InspectionWorkspace({ inspectionId }: { inspectionId: string }) {
  const { data: inspection, isLoading: loadingInspection } = useInspection(inspectionId);
  const { data: images } = useInspectionImages(inspectionId);
  const { data: violations } = useViolations(inspectionId);
  const { data: evidence } = useEvidence(inspectionId);
  const { data: traceSteps = [] } = useDecisionTrace(inspectionId);
  const { runPipeline, phase, isRunning, error, warnings } = useRunPipeline(inspectionId);
  const [dismissedWarnings, setDismissedWarnings] = useState(false);

  const [selectedImageId, setSelectedImageId] = useState<string | null>(null);
  // Keyed by violation id everywhere (not evidence id) — the right panel selects
  // by violation, and a violation is the one stable identity shared between
  // "which row is highlighted in the results panel" and "which box is
  // highlighted on the image" and "which rule Nirikshak should explain."
  const [selectedViolationId, setSelectedViolationId] = useState<string | null>(null);
  const [copilotOpen, setCopilotOpen] = useState(false);
  /** Below `lg`, the 3-column grid becomes a segmented single-panel switcher — all three panels stay mounted (no re-fetching, no duplicate dialogs), only visibility toggles via CSS. */
  const [mobilePanel, setMobilePanel] = useState<"capture" | "image" | "results">("capture");

  const missingRequiredAngles = MINIMUM_REQUIRED_ANGLES.filter(
    (angle) => !images?.some((img) => img.imageType === angle),
  );

  /**
   * Barcode-first workflow: a scanned product with known master data
   * doesn't need a manual "Run AI Pipeline" press, a refresh, or a
   * confirmation popup — it goes straight to rule evaluation the moment
   * the scan resolves. `skipExtraction: true` means fusion/evaluation/
   * evidence run immediately without waiting on OCR/Vision calls whose
   * answers the product's own data would override anyway (see
   * `DeclarationFusionServiceImpl`); switching to the results panel on
   * mobile is what makes the outcome actually visible without another tap.
   */
  function handleProductMatched() {
    setDismissedWarnings(false);
    runPipeline({ images: images ?? [], skipExtraction: true });
    setMobilePanel("results");
  }

  const activeImage = useMemo(
    () => images?.find((img) => img.id === selectedImageId) ?? images?.[0] ?? null,
    [images, selectedImageId],
  );

  const overlayBoxes = useMemo<OverlayBox[]>(() => {
    if (!activeImage || !evidence) return [];
    return evidence
      .filter((e) => e.imageId === activeImage.id && e.boundingBox)
      .map((e) => {
        const violation = violations?.find((v) => v.id === e.violationId);
        return {
          id: e.violationId,
          box: e.boundingBox!,
          label: violation?.ruleCode ?? "Violation",
          severity: e.severity,
          confidence: e.fusedConfidence,
        };
      });
  }, [activeImage, evidence, violations]);

  const selectedRuleCode = useMemo(
    () => violations?.find((v) => v.id === selectedViolationId)?.ruleCode ?? null,
    [violations, selectedViolationId],
  );

  if (loadingInspection) {
    return (
      <div className="flex h-full items-center justify-center">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!inspection) {
    return (
      <EmptyState icon={ImageIcon} title="Inspection not found" className="h-full" />
    );
  }

  return (
    <div className="relative flex h-full flex-col">
      {/* Workspace header */}
      <div className="relative flex shrink-0 flex-col gap-2 overflow-hidden border-b border-border bg-surface-raised/70 px-3 py-2.5 backdrop-blur-sm sm:flex-row sm:items-center sm:justify-between sm:px-4">
        <div
          className="pointer-events-none absolute inset-x-0 top-0 h-px opacity-60"
          style={{ backgroundImage: "var(--gradient-ai)" }}
        />
        <div className="flex items-center gap-3">
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="text-sm font-semibold text-foreground">
                {inspectionDisplayName(inspection)}
              </h1>
              <StatusBadge status={inspection.status} />
              <AnimatePresence>
                {isRunning && (
                  <motion.span
                    initial={{ opacity: 0, scale: 0.9 }}
                    animate={{ opacity: 1, scale: 1 }}
                    exit={{ opacity: 0, scale: 0.9 }}
                    className="flex items-center gap-1 rounded-full bg-accent-soft px-2 py-0.5 text-[10px] font-medium text-accent-soft-foreground"
                  >
                    <span className="size-1.5 animate-pulse rounded-full bg-current" />
                    Pipeline running
                  </motion.span>
                )}
              </AnimatePresence>
            </div>
            <div className="mt-0.5 flex items-center gap-3 text-[11px] text-faint-foreground">
              <span className="font-mono">{inspection.id.slice(0, 8)}</span>
              {inspection.region && (
                <span className="flex items-center gap-1">
                  <MapPin className="size-3" /> {inspection.region}
                </span>
              )}
              <span>Inspector: {inspection.inspectorName}</span>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {error && <span className="max-w-xs text-xs text-critical">{error}</span>}
          <Button
            variant="ai"
            size="sm"
            disabled={missingRequiredAngles.length > 0 || isRunning}
            title={
              missingRequiredAngles.length > 0
                ? `Capture ${missingRequiredAngles.map((a) => a.toLowerCase()).join(" and ")} first`
                : undefined
            }
            onClick={() => {
              setDismissedWarnings(false);
              if (images) runPipeline({ images });
            }}
          >
            {isRunning ? <Loader2 className="size-3.5 animate-spin" /> : <Play className="size-3.5" />}
            Run AI Pipeline
          </Button>
        </div>
      </div>

      {/* Non-fatal per-provider warnings (e.g. OCR down but Vision AI carried the run) — distinct from `error`, which is a fatal abort */}
      <AnimatePresence>
        {warnings.length > 0 && !dismissedWarnings && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            className="shrink-0 overflow-hidden border-b border-warning/30 bg-warning-soft"
          >
            <div className="flex items-start gap-2 px-4 py-2">
              <AlertTriangle className="mt-0.5 size-3.5 shrink-0 text-warning" />
              <div className="min-w-0 flex-1">
                <p className="text-xs font-medium text-warning-foreground">
                  Pipeline completed with {warnings.length} warning{warnings.length > 1 ? "s" : ""} — results below use only the sources that succeeded.
                </p>
                <ul className="mt-1 space-y-0.5">
                  {warnings.map((w, i) => (
                    <li key={i} className="text-[11px] text-warning-foreground/80">{w}</li>
                  ))}
                </ul>
              </div>
              <button onClick={() => setDismissedWarnings(true)} className="shrink-0 text-warning-foreground/70 hover:text-warning-foreground">
                <X className="size-3.5" />
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Mobile panel switcher — lg and up use the 3-column grid below instead */}
      <div className="flex shrink-0 gap-1 border-b border-border bg-surface p-1.5 lg:hidden">
        {(
          [
            { key: "capture", label: "Capture", icon: Upload },
            { key: "image", label: "Image", icon: ScanEye },
            { key: "results", label: "Results", icon: ListChecks },
          ] as const
        ).map((tab) => (
          <button
            key={tab.key}
            onClick={() => setMobilePanel(tab.key)}
            className={cn(
              "flex flex-1 items-center justify-center gap-1.5 rounded-md py-1.5 text-xs font-medium transition-colors",
              mobilePanel === tab.key
                ? "bg-accent-soft text-accent-soft-foreground"
                : "text-muted-foreground hover:bg-surface-sunken",
            )}
          >
            <tab.icon className="size-3.5" />
            {tab.label}
            {tab.key === "results" && violations && violations.length > 0 && (
              <span className="rounded-full bg-critical px-1.5 text-[10px] text-white">{violations.length}</span>
            )}
          </button>
        ))}
      </div>

      {/*
        Three-panel workspace — single-panel switcher on mobile, fixed
        3-column grid from lg up. `grid-rows-1` (→ `grid-template-rows:
        repeat(1,minmax(0,1fr))`) matters on mobile specifically: with the
        default `auto` row sizing, a single visible grid item (the other
        two are `hidden`, removed from layout) sized its row to its own
        content instead of the grid's actual available height — so a
        violations list longer than one screen just grew past the visible
        area with nothing to scroll it into view, and the outer page
        can't scroll either (by design, see AppLayout). `minmax(0,1fr)`
        forces the row to the container's real height, which is what lets
        each panel's own internal `overflow-y-auto` do its job at all.
      */}
      <div className="grid min-h-0 flex-1 grid-rows-1 grid-cols-1 lg:grid-cols-[280px_1fr_340px]">
        <motion.div
          initial={{ opacity: 0, x: -8 }}
          animate={{ opacity: 1, x: 0 }}
          className={cn("h-full min-h-0 border-border lg:border-r", mobilePanel === "capture" ? "block" : "hidden lg:block")}
        >
          <ImageCapturePanel
            inspectionId={inspectionId}
            selectedImageId={activeImage?.id ?? null}
            onSelectImage={setSelectedImageId}
            onProductMatched={handleProductMatched}
          />
        </motion.div>

        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className={cn("h-full min-h-0", mobilePanel === "image" ? "block" : "hidden lg:block")}
        >
          {activeImage ? (
            <AnnotatedImageViewer
              imageUrl={activeImage.signedUrl}
              boxes={overlayBoxes}
              selectedBoxId={selectedViolationId}
              onSelectBox={setSelectedViolationId}
            />
          ) : (
            <EmptyState
              icon={ImageIcon}
              title="No image selected"
              description="Upload a label photo to begin analysis."
              className="h-full"
            />
          )}
        </motion.div>

        <motion.div
          initial={{ opacity: 0, x: 8 }}
          animate={{ opacity: 1, x: 0 }}
          className={cn("h-full min-h-0 border-border lg:border-l", mobilePanel === "results" ? "block" : "hidden lg:block")}
        >
          <InspectionResultsPanel inspection={inspection} images={images ?? []} onSelectViolation={setSelectedViolationId} />
        </motion.div>
      </div>

      {/* Bottom decision timeline */}
      <div className="h-14 shrink-0 border-t border-border bg-surface">
        <DecisionTimeline steps={traceSteps} currentPhase={phase} />
      </div>

      <NirikshakFab onClick={() => setCopilotOpen(true)} />
      <Sheet open={copilotOpen} onOpenChange={setCopilotOpen}>
        {copilotOpen && <ChatPanel inspectionId={inspectionId} selectedRuleCode={selectedRuleCode} />}
      </Sheet>
    </div>
  );
}
