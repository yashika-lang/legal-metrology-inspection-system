import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { pipelineApi } from "../api/pipelineApi";
import { queryKeys } from "@/constants/queryKeys";
import type { ImageResponse } from "../types";

export function useOcrHistory(imageId: string) {
  return useQuery({
    queryKey: queryKeys.imageOcrHistory(imageId),
    queryFn: () => pipelineApi.getOcrHistory(imageId),
    enabled: !!imageId,
  });
}

export function useVisionDetections(imageId: string) {
  return useQuery({
    queryKey: queryKeys.imageVisionDetections(imageId),
    queryFn: () => pipelineApi.getVisionDetections(imageId),
    enabled: !!imageId,
  });
}

export function useFusedDeclarations(inspectionId: string) {
  return useQuery({
    queryKey: queryKeys.fusedDeclarations(inspectionId),
    queryFn: () => pipelineApi.getFusedDeclarations(inspectionId),
    enabled: !!inspectionId,
  });
}

export function useViolations(inspectionId: string) {
  return useQuery({
    queryKey: queryKeys.violations(inspectionId),
    queryFn: () => pipelineApi.getViolations(inspectionId),
    enabled: !!inspectionId,
  });
}

export function useEvaluationResults(inspectionId: string) {
  return useQuery({
    queryKey: queryKeys.evaluationResults(inspectionId),
    queryFn: () => pipelineApi.getEvaluationResults(inspectionId),
    enabled: !!inspectionId,
  });
}

export function useEvidence(inspectionId: string) {
  return useQuery({
    queryKey: queryKeys.evidence(inspectionId),
    queryFn: () => pipelineApi.getEvidence(inspectionId),
    enabled: !!inspectionId,
  });
}

export function useDecisionTrace(inspectionId: string) {
  return useQuery({
    queryKey: queryKeys.decisionTrace(inspectionId),
    queryFn: () => pipelineApi.getDecisionTrace(inspectionId),
    enabled: !!inspectionId,
    refetchInterval: (query) => (query.state.data ? false : 2000),
  });
}

export type PipelinePhase =
  | "idle"
  | "ocr"
  | "vision"
  | "fusion"
  | "evaluation"
  | "evidence"
  | "done"
  | "error";

function backendMessage(err: unknown, fallback: string): string {
  const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
  return message ?? fallback;
}

interface PipelineResult {
  evaluation: Awaited<ReturnType<typeof pipelineApi.evaluateRules>>;
  warnings: string[];
}

/**
 * There is no backend pipeline-orchestrator endpoint (by design — see
 * docs/ARCHITECTURE.md's AI Pipeline section; each stage is its own
 * on-demand endpoint) so the workspace itself drives the sequence:
 * OCR + Vision per image, then one fuse → evaluate → evidence pass for
 * the inspection. Every call here hits a real endpoint; nothing is
 * mocked. `phase` drives the Decision Timeline's live state.
 *
 * OCR and Vision are two *independent* evidence sources by design (see
 * DeclarationFusionService) — one of them being unavailable (an external
 * provider outage, quota, or misconfiguration) must not abort the whole
 * pipeline when the other can still produce a usable result. Only
 * fusion/evaluation/evidence — the calls with no fallback source — are
 * treated as fatal.
 */
export function useRunPipeline(inspectionId: string) {
  const queryClient = useQueryClient();
  const [phase, setPhase] = useState<PipelinePhase>("idle");
  const [error, setError] = useState<string | null>(null);
  const [warnings, setWarnings] = useState<string[]>([]);

  const mutation = useMutation({
    mutationFn: async ({ images, skipExtraction }: { images: ImageResponse[]; skipExtraction?: boolean }): Promise<PipelineResult> => {
      setError(null);
      setWarnings([]);
      const runWarnings: string[] = [];

      /*
       * Barcode-first workflow: a scanned product with known master data
       * doesn't need OCR/Vision AI to re-derive facts that are already
       * certain — `DeclarationFusionService` gives a linked product's
       * declarations top priority over anything OCR/Vision would produce
       * for the same field regardless, so skipping straight to fusion
       * here is purely a speed/cost win, not a behavior change. Any
       * declaration type the product's data *doesn't* cover still comes
       * from OCR/Vision normally — this fast path just means an inspector
       * isn't waiting on OCR/Vision calls whose answers would be
       * overridden anyway.
       */
      if (!skipExtraction) {
        /*
         * OCR/Vision for different images are fully independent calls (each
         * addresses its own image id, no shared state) — there was never a
         * reason to `await` them one at a time. Found live: running them
         * sequentially made a 3-4 image inspection take minutes, since each
         * call's real latency (a Google Vision/Gemini round trip, several
         * seconds to tens of seconds under normal provider load) summed
         * instead of overlapping. `Promise.allSettled` runs every image's
         * call at once and still collects a per-image warning on failure,
         * exactly as the previous sequential loop did — this only changes
         * how the requests are scheduled, not the fallback behavior.
         */
        setPhase("ocr");
        const ocrResults = await Promise.allSettled(images.map((image) => pipelineApi.runOcr(image.id)));
        ocrResults.forEach((result, i) => {
          if (result.status === "rejected") {
            runWarnings.push(`OCR unavailable for ${images[i].imageType.toLowerCase()} image: ${backendMessage(result.reason, "unknown error")}`);
          }
        });

        setPhase("vision");
        const visionResults = await Promise.allSettled(images.map((image) => pipelineApi.runVision(image.id)));
        visionResults.forEach((result, i) => {
          if (result.status === "rejected") {
            runWarnings.push(`Vision AI unavailable for ${images[i].imageType.toLowerCase()} image: ${backendMessage(result.reason, "unknown error")}`);
          }
        });

        /*
         * Per explicit user direction: never hard-block the run here, even
         * when every OCR/Vision call failed (e.g. a temporary Gemini rate
         * limit) — the pipeline must always complete to a visible result
         * rather than stopping on a fatal error banner. Any all-failed case
         * still shows up as per-image warnings above, and — for a product
         * whose barcode was scanned — fusion still has real, correct data
         * to work with regardless of whether OCR/Vision produced anything
         * at all (see `DeclarationFusionServiceImpl`, which gives a scanned
         * product's own verified data top priority). For an unscanned
         * product with no working OCR/Vision, the Rule Engine will
         * correctly show missing fields as violations — visible and
         * explained in the warnings above, not hidden behind a block.
         */
      }

      setPhase("fusion");
      await pipelineApi.fuseDeclarations(inspectionId);

      setPhase("evaluation");
      const evaluation = await pipelineApi.evaluateRules(inspectionId);

      setPhase("evidence");
      await pipelineApi.generateEvidence(inspectionId);

      setPhase("done");
      setWarnings(runWarnings);
      return { evaluation, warnings: runWarnings };
    },
    onError: (err) => {
      setPhase("error");
      setError(backendMessage(err, err instanceof Error ? err.message : "Pipeline run failed"));
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.inspection(inspectionId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.fusedDeclarations(inspectionId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.violations(inspectionId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.evaluationResults(inspectionId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.evidence(inspectionId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.decisionTrace(inspectionId) });
    },
  });

  return { runPipeline: mutation.mutate, phase, error, warnings, isRunning: mutation.isPending };
}
