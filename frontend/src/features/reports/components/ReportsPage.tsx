import { useState } from "react";
import { FileText, Download, FileType2, Sparkles, Loader2, ChevronLeft, ClipboardList } from "lucide-react";
import { motion } from "framer-motion";
import { PageHeader } from "@/components/common/PageHeader";
import { Pagination } from "@/components/common/Pagination";
import { EmptyState } from "@/components/common/EmptyState";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { StatusBadge } from "@/components/common/StatusBadge";
import { SeverityBadge } from "@/components/common/SeverityBadge";
import { scoreToColor } from "@/components/charts/ComplianceGauge";
import { cn } from "@/lib/cn";
import { useInspections } from "@/features/inspections/hooks/useInspection";
import { inspectionDisplayName } from "@/features/inspections/utils/displayName";
import { useReportsByInspection, useReportJson, useGenerateReport } from "../hooks/useReports";

export function ReportsPage() {
  const [page, setPage] = useState(0);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const { data, isLoading } = useInspections({ page, size: 15, sort: "startedAt,desc", evaluatedOnly: true });

  return (
    <div className="flex h-full overflow-hidden">
      <div className={cn("flex min-w-0 flex-1 flex-col border-border lg:w-96 lg:flex-none lg:border-r", selectedId && "hidden lg:flex")}>
        <PageHeader title="Reports" description="Generate and download Smart Reports per inspection." />
        <div className="min-h-0 flex-1 overflow-y-auto">
          {isLoading ? (
            <div className="space-y-2 p-4">{Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-14 w-full" />)}</div>
          ) : !data || data.items.length === 0 ? (
            <EmptyState
              icon={FileText}
              title="No evaluated inspections yet"
              description="Run the AI Pipeline on an inspection to see its report here."
              className="h-full"
            />
          ) : (
            <ul className="divide-y divide-border">
              {data.items.map((inspection) => (
                <li key={inspection.id}>
                  <button
                    onClick={() => setSelectedId(inspection.id)}
                    className={cn(
                      "flex w-full items-center justify-between gap-2 px-4 py-3 text-left transition-colors hover:bg-surface-sunken",
                      selectedId === inspection.id && "bg-accent-soft",
                    )}
                  >
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium text-foreground">{inspectionDisplayName(inspection)}</p>
                      <p className="mt-0.5 font-mono text-[10px] text-faint-foreground">{inspection.id.slice(0, 8)}</p>
                    </div>
                    <StatusBadge status={inspection.status} />
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
        {data && <Pagination page={data.page} totalPages={data.totalPages} totalItems={data.totalItems} onPageChange={setPage} />}
      </div>

      <div className={cn("min-h-0 min-w-0 flex-1 overflow-y-auto", !selectedId && "hidden lg:block")}>
        {selectedId ? (
          <ReportDetail inspectionId={selectedId} onBack={() => setSelectedId(null)} />
        ) : (
          <EmptyState icon={FileText} title="Select an inspection" description="Choose one on the left to view or generate its report." className="h-full" />
        )}
      </div>
    </div>
  );
}

function ReportDetail({ inspectionId, onBack }: { inspectionId: string; onBack: () => void }) {
  const { data: persistedReports, isLoading: loadingReports } = useReportsByInspection(inspectionId);
  const { data: preview, isLoading: loadingPreview } = useReportJson(inspectionId);
  const generateReport = useGenerateReport(inspectionId);

  return (
    <div className="p-4 sm:p-6">
      <Button variant="ghost" size="sm" className="mb-3 lg:hidden" onClick={onBack}>
        <ChevronLeft className="size-3.5" /> Back to list
      </Button>

      <div className="mb-6 flex items-center justify-between gap-3">
        <div>
          <h2 className="text-base font-semibold text-foreground">Smart Report</h2>
          <p className="font-mono text-xs text-faint-foreground">{inspectionId}</p>
        </div>
        <Button variant="ai" size="sm" disabled={generateReport.isPending} onClick={() => generateReport.mutate()}>
          {generateReport.isPending ? <Loader2 className="size-3.5 animate-spin" /> : <Sparkles className="size-3.5" />}
          Generate Report
        </Button>
      </div>

      {generateReport.isError && (
        <p className="mb-4 rounded-md bg-critical-soft px-3 py-2 text-xs text-critical-foreground">
          Could not generate the report — the inspection may not have any evaluated violations/evidence yet.
        </p>
      )}

      <div className="mb-6">
        <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">Persisted Reports (PDF/DOCX)</p>
        {loadingReports ? (
          <Skeleton className="h-16 w-full" />
        ) : !persistedReports || persistedReports.length === 0 ? (
          <p className="text-xs text-muted-foreground">No report generated yet — click "Generate Report" above.</p>
        ) : (
          <ul className="space-y-2">
            {persistedReports.map((report) => (
              <motion.li
                key={report.id}
                initial={{ opacity: 0, y: 4 }}
                animate={{ opacity: 1, y: 0 }}
                className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-border bg-surface-raised p-3"
              >
                <div>
                  <p className="text-sm font-medium text-foreground">{report.reportNumber}</p>
                  <p className="text-[11px] text-faint-foreground">
                    Generated by {report.generatedByName} · {new Date(report.generatedAt).toLocaleString()}
                  </p>
                </div>
                <div className="flex gap-1.5">
                  <Button variant="outline" size="sm" asChild>
                    <a href={report.pdfUrl} target="_blank" rel="noreferrer">
                      <Download className="size-3.5" /> PDF
                    </a>
                  </Button>
                  <Button variant="outline" size="sm" asChild>
                    <a href={report.docxUrl} target="_blank" rel="noreferrer">
                      <FileType2 className="size-3.5" /> DOCX
                    </a>
                  </Button>
                </div>
              </motion.li>
            ))}
          </ul>
        )}
      </div>

      <div>
        <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">Live Preview</p>
        {loadingPreview ? (
          <Skeleton className="h-64 w-full" />
        ) : !preview ? (
          <EmptyState icon={ClipboardList} title="Preview unavailable" />
        ) : (
          <div className="space-y-4 rounded-lg border border-border bg-surface-raised p-4">
            <div className="flex flex-wrap items-center gap-4">
              <div>
                <p className="text-[10px] uppercase text-faint-foreground">Compliance Score</p>
                <p
                  className="tabular text-2xl font-semibold"
                  style={{ color: scoreToColor(preview.compliance.complianceScore) }}
                >
                  {preview.compliance.complianceScore != null ? Math.round(preview.compliance.complianceScore) : "—"}
                </p>
              </div>
              <div>
                <p className="text-[10px] uppercase text-faint-foreground">Violations</p>
                <p className="tabular text-2xl font-semibold text-foreground">{preview.compliance.violationCount}</p>
              </div>
              <div>
                <p className="text-[10px] uppercase text-faint-foreground">Critical</p>
                <p className="tabular text-2xl font-semibold text-critical">{preview.compliance.criticalCount}</p>
              </div>
            </div>

            <div>
              <p className="mb-1 text-xs font-semibold text-foreground">Executive Summary</p>
              <p className="whitespace-pre-wrap text-xs text-foreground-secondary">{preview.executiveSummary}</p>
            </div>

            <div>
              <p className="mb-1 text-xs font-semibold text-foreground">Recommendations</p>
              <p className="whitespace-pre-wrap text-xs text-foreground-secondary">{preview.recommendations}</p>
            </div>

            {preview.violations.length > 0 && (
              <div>
                <p className="mb-1.5 text-xs font-semibold text-foreground">Violations</p>
                <ul className="space-y-1.5">
                  {preview.violations.map((v) => (
                    <li key={v.violationId} className="flex items-center justify-between gap-2 rounded-md bg-surface-sunken px-2.5 py-1.5 text-xs">
                      <span className="font-mono text-[11px] text-faint-foreground">{v.ruleCode}</span>
                      <span className="flex-1 truncate px-2 text-foreground-secondary">{v.ruleTitle}</span>
                      <SeverityBadge severity={v.severity} />
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
