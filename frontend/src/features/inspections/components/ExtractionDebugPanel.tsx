import { CheckCircle2, XCircle, FileText, Sparkles, Layers, ListChecks } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { ConfidenceChip } from "@/components/common/ConfidenceChip";
import { SourceBadge } from "@/components/common/SourceBadge";
import { fusedValueDisplay } from "../utils/fusedValueDisplay";
import { DECLARATION_TYPE_LABEL } from "@/types/enums";
import { useOcrHistory, useVisionDetections } from "../hooks/usePipeline";
import type { ImageResponse, FusedDeclarationResponse } from "../types";
import type { ValidationResultResponse } from "../types";
import { cn } from "@/lib/cn";

/**
 * Answers "why did the pipeline say this field is missing?" with the real
 * data at every stage, instead of asking for trust: the exact OCR text and
 * Vision AI detections that came back per image, the fused value each
 * declaration settled on and which source(s) it came from, and every
 * rule's pass/fail reasoning against those fused values — not just the
 * violations. Nothing here is computed client-side; every section is a
 * direct read of an existing real endpoint (OCR history, Vision
 * detections, fused declarations, rule evaluation).
 */
export function ExtractionDebugPanel({
  images,
  declarations,
  evaluationResults,
}: {
  images: ImageResponse[];
  declarations: FusedDeclarationResponse[] | undefined;
  evaluationResults: ValidationResultResponse[] | undefined;
}) {
  return (
    <div className="space-y-6">
      <section>
        <SectionHeader icon={FileText} title="Raw extraction, per image" />
        {images.length === 0 ? (
          <EmptyState icon={FileText} title="No images uploaded yet" className="py-6" />
        ) : (
          <div className="space-y-3">
            {images.map((image) => (
              <PerImageExtraction key={image.id} image={image} />
            ))}
          </div>
        )}
      </section>

      <section>
        <SectionHeader icon={Layers} title="Fused declarations" description="The single best value the fusion step settled on for each field, and which source(s) agreed." />
        {!declarations || declarations.length === 0 ? (
          <EmptyState icon={Layers} title="No fused declarations yet" description="Run the pipeline through the fusion step to see this." className="py-6" />
        ) : (
          <div className="overflow-x-auto rounded-lg border border-border">
            <table className="w-full text-xs">
              <thead className="bg-surface-sunken text-[10px] uppercase text-muted-foreground">
                <tr>
                  <th className="px-2.5 py-2 text-left">Field</th>
                  <th className="px-2.5 py-2 text-left">Present</th>
                  <th className="px-2.5 py-2 text-left">Source</th>
                  <th className="px-2.5 py-2 text-left">Fused value</th>
                  <th className="px-2.5 py-2 text-left">OCR value</th>
                  <th className="px-2.5 py-2 text-left">Vision value</th>
                  <th className="px-2.5 py-2 text-left">Agreement</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {declarations.map((d) => (
                  <tr key={d.id}>
                    <td className="px-2.5 py-2 font-medium text-foreground">{DECLARATION_TYPE_LABEL[d.declarationType] ?? d.declarationType}</td>
                    <td className="px-2.5 py-2">
                      {d.present ? <CheckCircle2 className="size-3.5 text-success" /> : <XCircle className="size-3.5 text-critical" />}
                    </td>
                    <td className="px-2.5 py-2">
                      <SourceBadge ocrValue={d.ocrValue} visionValue={d.visionValue} fromProductDatabase={d.fromProductDatabase} />
                    </td>
                    <td className="px-2.5 py-2">
                      <div className="flex items-center gap-1.5">
                        <span className="text-foreground-secondary">{fusedValueDisplay(d)}</span>
                        {d.fusedConfidence != null && <ConfidenceChip confidence={d.fusedConfidence} />}
                      </div>
                    </td>
                    <td className="px-2.5 py-2 text-faint-foreground">
                      {d.ocrValue ?? "—"} {d.ocrConfidence != null && <span className="tabular">({Math.round(d.ocrConfidence * 100)}%)</span>}
                    </td>
                    <td className="px-2.5 py-2 text-faint-foreground">
                      {d.visionValue ?? "—"} {d.visionConfidence != null && <span className="tabular">({Math.round(d.visionConfidence * 100)}%)</span>}
                    </td>
                    <td className="px-2.5 py-2">
                      {d.agreement == null ? (
                        <span className="text-faint-foreground">n/a</span>
                      ) : d.agreement ? (
                        <Badge variant="success">Agree</Badge>
                      ) : (
                        <Badge variant="warning">Disagree</Badge>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section>
        <SectionHeader icon={ListChecks} title="Rule evaluation — every rule, pass or fail" description="Not just violations: this is every rule the engine actually checked, and why." />
        {!evaluationResults || evaluationResults.length === 0 ? (
          <EmptyState icon={ListChecks} title="No evaluation yet" description="Run the pipeline through rule evaluation to see this." className="py-6" />
        ) : (
          <div className="space-y-1.5">
            {evaluationResults.map((r) => (
              <div
                key={r.ruleId}
                className={cn(
                  "rounded-lg border px-3 py-2 text-xs",
                  r.passed ? "border-border bg-surface-raised" : "border-critical/30 bg-critical-soft",
                )}
              >
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-1.5 font-medium text-foreground">
                    {r.passed ? <CheckCircle2 className="size-3.5 text-success" /> : <XCircle className="size-3.5 text-critical" />}
                    <span className="font-mono text-[10px] text-faint-foreground">{r.ruleCode}</span>
                    {r.ruleTitle}
                  </div>
                  {r.confidence != null && <ConfidenceChip confidence={r.confidence} />}
                </div>
                <p className="mt-1 text-foreground-secondary">{r.explanation}</p>
                {(r.actualValue || r.expectedValue) && (
                  <p className="mt-1 text-[11px] text-faint-foreground">
                    Actual: <span className="font-medium">{r.actualValue ?? "—"}</span> · Expected:{" "}
                    <span className="font-medium">{r.expectedValue ?? "—"}</span>
                  </p>
                )}
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function PerImageExtraction({ image }: { image: ImageResponse }) {
  const { data: ocrHistory, isLoading: loadingOcr } = useOcrHistory(image.id);
  const { data: visionDetections, isLoading: loadingVision } = useVisionDetections(image.id);
  const latestOcr = ocrHistory?.[0];

  return (
    <div className="rounded-lg border border-border p-3">
      <div className="mb-2 flex items-center gap-2">
        <Badge variant="outline" className="uppercase">{image.imageType}</Badge>
        <span className="font-mono text-[10px] text-faint-foreground">{image.id.slice(0, 8)}</span>
      </div>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <div>
          <p className="mb-1 flex items-center gap-1 text-[10px] font-semibold uppercase text-muted-foreground">
            <FileText className="size-3" /> Raw OCR
          </p>
          {loadingOcr ? (
            <Skeleton className="h-16 w-full" />
          ) : !latestOcr ? (
            <p className="rounded-md bg-surface-sunken px-2 py-1.5 text-[11px] text-faint-foreground">
              No OCR result for this image — the provider call never succeeded (see the pipeline's error/warning banner for why).
            </p>
          ) : (
            <div className="space-y-1 rounded-md bg-surface-sunken px-2 py-1.5">
              <p className="whitespace-pre-wrap font-mono text-[11px] text-foreground-secondary">{latestOcr.correctedText || latestOcr.rawText || "(empty)"}</p>
              {latestOcr.confidence != null && (
                <div className="flex items-center gap-1">
                  <span className="text-[10px] text-faint-foreground">Confidence</span>
                  <ConfidenceChip confidence={latestOcr.confidence} />
                </div>
              )}
            </div>
          )}
        </div>

        <div>
          <p className="mb-1 flex items-center gap-1 text-[10px] font-semibold uppercase text-muted-foreground">
            <Sparkles className="size-3" /> Raw Vision AI detections
          </p>
          {loadingVision ? (
            <Skeleton className="h-16 w-full" />
          ) : !visionDetections || visionDetections.length === 0 ? (
            <p className="rounded-md bg-surface-sunken px-2 py-1.5 text-[11px] text-faint-foreground">
              No Vision AI detections for this image — the provider call never succeeded (see the pipeline's error/warning banner for why).
            </p>
          ) : (
            <ul className="space-y-1 rounded-md bg-surface-sunken px-2 py-1.5">
              {visionDetections.map((d) => (
                <li key={d.id} className="flex items-center justify-between gap-2 text-[11px]">
                  <span className="text-foreground-secondary">
                    {DECLARATION_TYPE_LABEL[d.declarationType] ?? d.declarationType}: {d.present ? (d.detectedValue ?? "(present, no value)") : "not found"}
                  </span>
                  {d.confidence != null && <ConfidenceChip confidence={d.confidence} />}
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}

function SectionHeader({ icon: Icon, title, description }: { icon: typeof FileText; title: string; description?: string }) {
  return (
    <div className="mb-2">
      <p className="flex items-center gap-1.5 text-xs font-semibold text-foreground">
        <Icon className="size-3.5 text-accent" /> {title}
      </p>
      {description && <p className="mt-0.5 text-[11px] text-faint-foreground">{description}</p>}
    </div>
  );
}
