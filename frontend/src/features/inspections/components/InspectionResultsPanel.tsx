import { useMemo, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { motion, AnimatePresence } from "framer-motion";
import {
  ShieldCheck,
  ShieldAlert,
  CheckCircle2,
  XCircle,
  FileImage,
  Fingerprint,
  Sparkles,
  Loader2,
  Play,
  ClipboardList,
  BadgeCheck,
  Bug,
} from "lucide-react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { SeverityBadge } from "@/components/common/SeverityBadge";
import { ComplianceGauge } from "@/components/charts/ComplianceGauge";
import { DECLARATION_TYPE_LABEL } from "@/types/enums";
import type { InspectionResponse, ImageResponse } from "../types";
import { useFusedDeclarations, useViolations, useEvidence, useEvaluationResults } from "../hooks/usePipeline";
import { useRules } from "@/features/rules/hooks/useRules";
import { copilotApi } from "@/features/ai-assistant/api/copilotApi";
import { scoreToColor } from "@/components/charts/ComplianceGauge";
import { ConfidenceChip } from "@/components/common/ConfidenceChip";
import { SourceBadge } from "@/components/common/SourceBadge";
import { ExtractionDebugPanel } from "./ExtractionDebugPanel";
import { fusedValueDisplay } from "../utils/fusedValueDisplay";
import { cn } from "@/lib/cn";

const FRAUD_RISK_CONFIG = {
  LOW: { label: "Low Risk", variant: "success" as const },
  MEDIUM: { label: "Medium Risk", variant: "warning" as const },
  HIGH: { label: "High Risk", variant: "critical" as const },
};

interface InspectionResultsPanelProps {
  inspection: InspectionResponse;
  images: ImageResponse[];
  onSelectViolation: (boxOwnerId: string | null) => void;
}

export function InspectionResultsPanel({ inspection, images, onSelectViolation }: InspectionResultsPanelProps) {
  const [tab, setTab] = useState("overview");
  const { data: declarations, isLoading: loadingDeclarations } = useFusedDeclarations(inspection.id);
  const { data: violations, isLoading: loadingViolations } = useViolations(inspection.id);
  const { data: evidence, isLoading: loadingEvidence } = useEvidence(inspection.id);
  const { data: evaluationResults } = useEvaluationResults(inspection.id);
  const { data: activeRules } = useRules({ activeOnly: true, size: 100 });

  const aiRecommendations = useMutation({
    mutationFn: () => copilotApi.manufacturerRecommendations(inspection.id),
  });

  /** The pipeline hasn't been run at all yet — `complianceScore` stays null until the Rule Engine evaluates at least once. This is the one reliable real signal, not a guess. */
  const pipelineHasRun = inspection.complianceScore != null;

  const violationCounts = useMemo(() => {
    const counts = { CRITICAL: 0, MAJOR: 0, MINOR: 0 };
    violations?.forEach((v) => counts[v.severity]++);
    return counts;
  }, [violations]);

  /** Cross-references real evaluation results (every rule checked, pass/fail) against the real Rules list's `mandatory` flag — never a client-side guess at which declarations matter. */
  const mandatoryDeclarationStatus = useMemo(() => {
    if (!evaluationResults || !activeRules) return [];
    const mandatoryRuleIds = new Set(activeRules.items.filter((r) => r.mandatory).map((r) => r.id));
    const byField = new Map<string, boolean>();
    evaluationResults
      .filter((r) => r.field && mandatoryRuleIds.has(r.ruleId))
      .forEach((r) => {
        const existing = byField.get(r.field!);
        byField.set(r.field!, existing === undefined ? r.passed : existing && r.passed);
      });
    return Array.from(byField.entries());
  }, [evaluationResults, activeRules]);

  return (
    <Tabs value={tab} onValueChange={setTab} className="flex h-full flex-col">
      {/* Sticky inspection summary — stays visible regardless of which tab is open, so the headline numbers are never scrolled out of view */}
      <div className="glass sticky top-0 z-10 flex shrink-0 items-center justify-between gap-3 border-b border-border px-4 py-2.5">
        {pipelineHasRun ? (
          <>
            <div className="flex items-center gap-1.5">
              <span className="tabular text-lg font-bold" style={{ color: scoreToColor(inspection.complianceScore) }}>
                {Math.round(inspection.complianceScore!)}
              </span>
              <span className="text-[10px] text-faint-foreground">/ 100</span>
            </div>
            <div className="flex items-center gap-1.5">
              {inspection.fraudRisk && (
                <Badge variant={FRAUD_RISK_CONFIG[inspection.fraudRisk].variant}>{FRAUD_RISK_CONFIG[inspection.fraudRisk].label}</Badge>
              )}
              {violations && violations.length > 0 && (
                <Badge variant="critical">{violations.length} violation{violations.length > 1 ? "s" : ""}</Badge>
              )}
            </div>
          </>
        ) : (
          <p className="text-xs text-muted-foreground">Awaiting AI Pipeline run</p>
        )}
      </div>

      <div className="border-b border-border px-3 pt-3">
        <TabsList className="w-full">
          <TabsTrigger value="overview" className="flex-1">Overview</TabsTrigger>
          <TabsTrigger value="fields" className="flex-1">Fields</TabsTrigger>
          <TabsTrigger value="violations" className="flex-1">
            Violations
            {violations && violations.length > 0 && (
              <span className="ml-1 rounded-full bg-critical px-1.5 text-[10px] text-white">{violations.length}</span>
            )}
          </TabsTrigger>
          <TabsTrigger value="evidence" className="flex-1">Evidence</TabsTrigger>
          <TabsTrigger value="debug" className="flex-1">
            <Bug className="size-3.5" />
          </TabsTrigger>
        </TabsList>
      </div>

      {/* pb-[max(1rem,env(safe-area-inset-bottom))]: on a notched/gesture-nav phone, the last violation card in a long list sat flush against (or behind) the home-indicator area with no way to scroll it fully into view — this reserves real clearance instead of assuming 1rem is always enough. */}
      <div className="min-h-0 flex-1 overflow-y-auto px-4 pb-[max(1rem,env(safe-area-inset-bottom))] pt-4">
        <TabsContent value="overview" className="mt-0 space-y-5">
          {!pipelineHasRun ? (
            <EmptyState
              icon={Play}
              title="Run the AI Pipeline"
              description="Compliance score, risk, violations, and declaration status all appear here once the pipeline evaluates this inspection's images."
              className="py-10"
            />
          ) : (
            <>
              <motion.div
                initial={{ opacity: 0, y: 6 }}
                animate={{ opacity: 1, y: 0 }}
                className="glass card-hover relative flex items-center justify-around overflow-hidden rounded-xl border border-border p-4 shadow-md"
              >
                <div
                  className="pointer-events-none absolute inset-x-0 top-0 h-0.5 opacity-70"
                  style={{ backgroundImage: "var(--gradient-ai)" }}
                />
                <ComplianceGauge score={inspection.complianceScore} size={104} />
                <div className="flex flex-col items-center gap-2">
                  <div
                    className={cn(
                      "flex size-14 items-center justify-center rounded-full",
                      inspection.fraudRisk === "HIGH" && "bg-critical-soft",
                      inspection.fraudRisk === "MEDIUM" && "bg-warning-soft",
                      (inspection.fraudRisk === "LOW" || !inspection.fraudRisk) && "bg-success-soft",
                    )}
                  >
                    {inspection.fraudRisk === "HIGH" ? (
                      <ShieldAlert className="size-6 text-critical" />
                    ) : (
                      <ShieldCheck className={cn("size-6", inspection.fraudRisk === "MEDIUM" ? "text-warning" : "text-success")} />
                    )}
                  </div>
                  <Badge variant={inspection.fraudRisk ? FRAUD_RISK_CONFIG[inspection.fraudRisk].variant : "neutral"}>
                    {inspection.fraudRisk ? FRAUD_RISK_CONFIG[inspection.fraudRisk].label : "Not Assessed"}
                  </Badge>
                  <span className="text-[10px] text-faint-foreground">Risk Score</span>
                </div>
              </motion.div>

              {/* Violation counts by severity */}
              <div>
                <SectionLabel icon={ShieldAlert} label="Violation Counts" />
                <div className="grid grid-cols-3 gap-2">
                  <CountChip label="Critical" count={violationCounts.CRITICAL} tone="critical" />
                  <CountChip label="Major" count={violationCounts.MAJOR} tone="warning" />
                  <CountChip label="Minor" count={violationCounts.MINOR} tone="info" />
                </div>
              </div>

              {/* Mandatory declaration status */}
              <div>
                <SectionLabel icon={BadgeCheck} label="Mandatory Declaration Status" />
                {!evaluationResults || !activeRules ? (
                  <Skeleton className="h-16 w-full" />
                ) : mandatoryDeclarationStatus.length === 0 ? (
                  <p className="text-xs text-muted-foreground">No mandatory-declaration rules matched this evaluation.</p>
                ) : (
                  <ul className="grid grid-cols-1 gap-1.5 sm:grid-cols-2">
                    {mandatoryDeclarationStatus.map(([field, ok]) => (
                      <li
                        key={field}
                        className={cn(
                          "flex items-center gap-1.5 rounded-md px-2 py-1.5 text-xs",
                          ok ? "bg-success-soft text-success-foreground" : "bg-critical-soft text-critical-foreground",
                        )}
                      >
                        {ok ? <CheckCircle2 className="size-3.5 shrink-0" /> : <XCircle className="size-3.5 shrink-0" />}
                        {DECLARATION_TYPE_LABEL[field as keyof typeof DECLARATION_TYPE_LABEL] ?? field}
                      </li>
                    ))}
                  </ul>
                )}
              </div>

              <div>
                <div className="mb-2.5 flex items-center justify-between">
                  <SectionLabel icon={Sparkles} label="AI Recommendations" />
                  <Button
                    variant="ai"
                    size="sm"
                    disabled={!violations || violations.length === 0 || aiRecommendations.isPending}
                    onClick={() => aiRecommendations.mutate()}
                  >
                    {aiRecommendations.isPending ? <Loader2 className="size-3 animate-spin" /> : <Sparkles className="size-3" />}
                    {aiRecommendations.data ? "Regenerate" : "Generate"}
                  </Button>
                </div>

                <AnimatePresence mode="wait">
                  {aiRecommendations.isPending ? (
                    <motion.div key="loading" exit={{ opacity: 0 }}>
                      <Skeleton className="h-20 w-full" />
                    </motion.div>
                  ) : aiRecommendations.data ? (
                    <motion.div
                      key="result"
                      initial={{ opacity: 0, y: 6 }}
                      animate={{ opacity: 1, y: 0 }}
                      className="space-y-1.5 rounded-lg border border-border bg-surface p-3"
                    >
                      <p className="whitespace-pre-wrap text-xs text-foreground-secondary">{aiRecommendations.data.answer}</p>
                      <p className="text-[10px] text-faint-foreground">
                        {aiRecommendations.data.generatedByAi
                          ? `Generated by ${aiRecommendations.data.provider}`
                          : "Deterministic answer"}
                      </p>
                    </motion.div>
                  ) : aiRecommendations.isError ? (
                    <p className="text-xs text-critical">The Copilot couldn't generate recommendations just now.</p>
                  ) : !violations || violations.length === 0 ? (
                    <p className="text-xs text-muted-foreground">No violations found — this inspection is fully compliant.</p>
                  ) : (
                    <p className="text-xs text-muted-foreground">
                      {violations.length} violation(s) found. Generate AI-written, manufacturer-facing recommendations from Nirikshak.
                    </p>
                  )}
                </AnimatePresence>
              </div>
            </>
          )}
        </TabsContent>

        <TabsContent value="fields" className="mt-0">
          <SectionLabel icon={FileImage} label="Detected Declarations" />
          {loadingDeclarations ? (
            <div className="space-y-2">
              {Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-9 w-full" />)}
            </div>
          ) : !declarations || declarations.length === 0 ? (
            <EmptyState icon={FileImage} title="No declarations yet" description="Run OCR and Vision AI to extract fields from the label." />
          ) : (
            <ul className="divide-y divide-border rounded-lg border border-border">
              {declarations.map((d) => (
                <li key={d.id} className="flex items-center justify-between gap-2 px-3 py-2.5">
                  <div className="flex min-w-0 items-center gap-2">
                    {d.present ? (
                      <CheckCircle2 className="size-3.5 shrink-0 text-success" />
                    ) : (
                      <XCircle className="size-3.5 shrink-0 text-critical" />
                    )}
                    <div className="min-w-0">
                      <span className="block text-xs font-medium text-foreground">{DECLARATION_TYPE_LABEL[d.declarationType]}</span>
                      <SourceBadge ocrValue={d.ocrValue} visionValue={d.visionValue} fromProductDatabase={d.fromProductDatabase} />
                    </div>
                  </div>
                  <div className="flex shrink-0 items-center gap-2 text-right">
                    <p className="max-w-24 truncate text-xs text-muted-foreground">{fusedValueDisplay(d)}</p>
                    <ConfidenceChip confidence={d.fusedConfidence} />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </TabsContent>

        <TabsContent value="violations" className="mt-0">
          <SectionLabel icon={ShieldAlert} label="Violations" />
          {loadingViolations ? (
            <Skeleton className="h-24 w-full" />
          ) : !violations || violations.length === 0 ? (
            <EmptyState
              icon={pipelineHasRun ? CheckCircle2 : ClipboardList}
              title={pipelineHasRun ? "No violations found" : "Pipeline not run yet"}
              description={pipelineHasRun ? "This inspection is fully compliant so far." : "Run the AI Pipeline to evaluate this inspection."}
            />
          ) : (
            <ul className="space-y-2">
              {violations.map((v, index) => (
                <motion.li
                  key={v.id}
                  initial={{ opacity: 0, y: 6 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.05 }}
                  onClick={() => onSelectViolation(v.id)}
                  className="card-hover cursor-pointer space-y-1.5 rounded-lg border border-border bg-surface p-3 hover:border-accent/40"
                >
                  <div className="flex items-center justify-between gap-2">
                    <span className="font-mono text-[11px] text-faint-foreground">{v.ruleCode}</span>
                    <div className="flex items-center gap-1.5">
                      <ConfidenceChip confidence={v.confidence} />
                      <SeverityBadge severity={v.severity} />
                    </div>
                  </div>
                  <p className="text-xs font-medium text-foreground">{v.ruleTitle}</p>
                  <p className="text-xs text-muted-foreground">{v.description}</p>
                  {v.suggestedFix && (
                    <p className="rounded-md bg-accent-soft px-2 py-1.5 text-[11px] text-accent-soft-foreground">
                      Fix: {v.suggestedFix}
                    </p>
                  )}
                </motion.li>
              ))}
            </ul>
          )}
        </TabsContent>

        <TabsContent value="evidence" className="mt-0">
          <SectionLabel icon={Fingerprint} label="Evidence" />
          {loadingEvidence ? (
            <Skeleton className="h-24 w-full" />
          ) : !evidence || evidence.length === 0 ? (
            <EmptyState icon={Fingerprint} title="No evidence generated" description="Evidence is generated once violations are evaluated." />
          ) : (
            <ul className="space-y-3">
              {evidence.map((e) => (
                <li key={e.id} className="overflow-hidden rounded-lg border border-border bg-surface">
                  {e.annotatedImageUrl && (
                    <img src={e.annotatedImageUrl} alt="Evidence" className="h-28 w-full object-cover" />
                  )}
                  <div className="space-y-1 p-2.5">
                    <div className="flex items-center justify-between">
                      <SeverityBadge severity={e.severity} />
                      {e.immutable && <Badge variant="outline">Immutable</Badge>}
                    </div>
                    <p className="text-xs text-muted-foreground">{e.reason}</p>
                    <p className="truncate font-mono text-[10px] text-faint-foreground" title={e.sha256Hash}>
                      SHA-256 {e.sha256Hash.slice(0, 16)}…
                    </p>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </TabsContent>

        <TabsContent value="debug" className="mt-0">
          <ExtractionDebugPanel images={images} declarations={declarations} evaluationResults={evaluationResults} />
        </TabsContent>
      </div>
    </Tabs>
  );
}

function CountChip({ label, count, tone }: { label: string; count: number; tone: "critical" | "warning" | "info" }) {
  const toneClasses = {
    critical: "bg-critical-soft text-critical-foreground",
    warning: "bg-warning-soft text-warning-foreground",
    info: "bg-info-soft text-info-foreground",
  };
  return (
    <div className={cn("rounded-lg px-2 py-2.5 text-center", toneClasses[tone])}>
      <p className="tabular text-lg font-semibold">{count}</p>
      <p className="text-[10px] uppercase tracking-wide opacity-80">{label}</p>
    </div>
  );
}

function SectionLabel({ icon: Icon, label }: { icon: typeof ShieldAlert; label: string }) {
  return (
    <div className="mb-2.5 flex items-center gap-1.5">
      <Icon className="size-3.5 text-muted-foreground" />
      <p className="text-xs font-semibold text-foreground">{label}</p>
    </div>
  );
}
