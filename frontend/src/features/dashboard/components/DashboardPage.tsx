import { Link } from "react-router-dom";
import {
  ClipboardList,
  CheckCircle2,
  ShieldAlert,
  Gauge,
  Percent,
  Users,
  ScrollText,
  AlertOctagon,
  Plus,
  ArrowRight,
  MapPin,
} from "lucide-react";
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
} from "recharts";
import { motion } from "framer-motion";
import { PageHeader } from "@/components/common/PageHeader";
import { StatCard } from "@/components/common/StatCard";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { StatusBadge } from "@/components/common/StatusBadge";
import { scoreToColor } from "@/components/charts/ComplianceGauge";
import { useKpis, useTimeseries, useSeverityDistribution } from "@/features/analytics/hooks/useAnalytics";
import { useInspections } from "@/features/inspections/hooks/useInspection";
import { inspectionDisplayName } from "@/features/inspections/utils/displayName";
import { ROUTES } from "@/constants/routes";

const SEVERITY_COLOR: Record<string, string> = {
  CRITICAL: "var(--color-critical)",
  MAJOR: "var(--color-warning)",
  MINOR: "var(--color-info)",
};

export function DashboardPage() {
  const { data: kpis, isLoading: loadingKpis } = useKpis();
  const { data: complianceTrend, isLoading: loadingTrend } = useTimeseries("compliance-score", 6);
  const { data: violationsTrend, isLoading: loadingViolations } = useTimeseries("violations", 6);
  const { data: severity, isLoading: loadingSeverity } = useSeverityDistribution();
  const { data: recentInspections, isLoading: loadingRecent } = useInspections({
    mine: true,
    size: 6,
    sort: "startedAt,desc",
    evaluatedOnly: true,
  });

  return (
    <div className="h-full overflow-y-auto">
      <PageHeader
        title="Dashboard"
        description="Live compliance metrics from every inspection you've run."
        actions={
          <Button variant="ai" size="sm" asChild>
            <Link to={ROUTES.newInspection}>
              <Plus className="size-3.5" />
              New Inspection
            </Link>
          </Button>
        }
      />

      <div className="space-y-6 p-4 sm:p-6">
        {/* KPI strip */}
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          {loadingKpis || !kpis ? (
            Array.from({ length: 8 }).map((_, i) => <Skeleton key={i} className="h-24" />)
          ) : (
            <>
              <StatCard icon={ClipboardList} label="Total Inspections" value={kpis.totalInspections} tone="accent" />
              <StatCard icon={CheckCircle2} label="Completed" value={kpis.completedInspections} tone="success" delay={0.03} />
              <StatCard icon={ShieldAlert} label="Violations" value={kpis.totalViolations} tone="warning" delay={0.06} />
              <StatCard icon={AlertOctagon} label="Critical Violations" value={kpis.criticalViolations} tone="critical" delay={0.09} />
              <StatCard
                icon={Gauge}
                label="Avg. Compliance Score"
                value={kpis.averageComplianceScore != null ? Math.round(kpis.averageComplianceScore) : "—"}
                suffix={kpis.averageComplianceScore != null ? "/ 100" : undefined}
                delay={0.12}
              />
              <StatCard
                icon={Percent}
                label="Compliance Rate"
                value={kpis.complianceRatePercent != null ? Math.round(kpis.complianceRatePercent) : "—"}
                suffix={kpis.complianceRatePercent != null ? "%" : undefined}
                tone="accent"
                delay={0.15}
              />
              <StatCard icon={Users} label="Active Inspectors" value={kpis.activeInspectors} delay={0.18} />
              <StatCard icon={ScrollText} label="Active Rules" value={kpis.activeRules} delay={0.21} />
            </>
          )}
        </div>

        <div className="grid grid-cols-1 gap-4 xl:grid-cols-3">
          {/* Compliance score trend */}
          <motion.div
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            className="rounded-xl border border-border bg-surface-raised p-4 xl:col-span-2"
          >
            <p className="mb-3 text-sm font-semibold text-foreground">Compliance Score Trend</p>
            {loadingTrend ? (
              <Skeleton className="h-56 w-full" />
            ) : !complianceTrend || complianceTrend.every((m) => m.count === 0) ? (
              <EmptyState icon={Gauge} title="No data yet" description="Run inspections to see the trend build up." className="h-56" />
            ) : (
              <ResponsiveContainer width="100%" height={224}>
                <AreaChart data={complianceTrend}>
                  <defs>
                    <linearGradient id="complianceFill" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="var(--color-ai-blue)" stopOpacity={0.35} />
                      <stop offset="100%" stopColor="var(--color-ai-blue)" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" vertical={false} />
                  <XAxis dataKey="month" tick={{ fontSize: 11, fill: "var(--color-faint-foreground)" }} axisLine={false} tickLine={false} />
                  <YAxis domain={[0, 100]} tick={{ fontSize: 11, fill: "var(--color-faint-foreground)" }} axisLine={false} tickLine={false} width={28} />
                  <Tooltip
                    contentStyle={{
                      background: "var(--color-surface-raised)",
                      border: "1px solid var(--color-border)",
                      borderRadius: 8,
                      fontSize: 12,
                    }}
                  />
                  <Area
                    type="monotone"
                    dataKey="averageValue"
                    name="Avg. Compliance Score"
                    stroke="var(--color-ai-blue)"
                    fill="url(#complianceFill)"
                    strokeWidth={2}
                  />
                </AreaChart>
              </ResponsiveContainer>
            )}
          </motion.div>

          {/* Severity distribution */}
          <motion.div
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.05 }}
            className="rounded-xl border border-border bg-surface-raised p-4"
          >
            <p className="mb-3 text-sm font-semibold text-foreground">Violation Severity</p>
            {loadingSeverity ? (
              <Skeleton className="h-56 w-full" />
            ) : !severity || severity.length === 0 || severity.every((s) => s.count === 0) ? (
              <EmptyState icon={ShieldAlert} title="No violations recorded" className="h-56" />
            ) : (
              <ResponsiveContainer width="100%" height={224}>
                <PieChart>
                  <Pie data={severity} dataKey="count" nameKey="label" innerRadius={55} outerRadius={80} paddingAngle={2}>
                    {severity.map((entry) => (
                      <Cell key={entry.label} fill={SEVERITY_COLOR[entry.label] ?? "var(--color-neutral-chip)"} />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{
                      background: "var(--color-surface-raised)",
                      border: "1px solid var(--color-border)",
                      borderRadius: 8,
                      fontSize: 12,
                    }}
                  />
                </PieChart>
              </ResponsiveContainer>
            )}
            <div className="mt-2 flex flex-wrap justify-center gap-3">
              {severity?.map((s) => (
                <span key={s.label} className="flex items-center gap-1.5 text-[11px] text-muted-foreground">
                  <span className="size-2 rounded-full" style={{ backgroundColor: SEVERITY_COLOR[s.label] ?? "var(--color-neutral-chip)" }} />
                  {s.label} ({s.count})
                </span>
              ))}
            </div>
          </motion.div>
        </div>

        <div className="grid grid-cols-1 gap-4 xl:grid-cols-3">
          {/* Violations trend */}
          <motion.div
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.08 }}
            className="rounded-xl border border-border bg-surface-raised p-4"
          >
            <p className="mb-3 text-sm font-semibold text-foreground">Violations / Month</p>
            {loadingViolations ? (
              <Skeleton className="h-48 w-full" />
            ) : !violationsTrend || violationsTrend.every((m) => m.count === 0) ? (
              <EmptyState icon={ShieldAlert} title="No violations yet" className="h-48" />
            ) : (
              <ResponsiveContainer width="100%" height={192}>
                <BarChart data={violationsTrend}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" vertical={false} />
                  <XAxis dataKey="month" tick={{ fontSize: 11, fill: "var(--color-faint-foreground)" }} axisLine={false} tickLine={false} />
                  <YAxis tick={{ fontSize: 11, fill: "var(--color-faint-foreground)" }} axisLine={false} tickLine={false} width={24} />
                  <Tooltip
                    contentStyle={{
                      background: "var(--color-surface-raised)",
                      border: "1px solid var(--color-border)",
                      borderRadius: 8,
                      fontSize: 12,
                    }}
                  />
                  <Bar dataKey="count" name="Violations" fill="var(--color-warning)" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </motion.div>

          {/* Recent inspections */}
          <motion.div
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            className="rounded-xl border border-border bg-surface-raised xl:col-span-2"
          >
            <div className="flex items-center justify-between border-b border-border px-4 py-3">
              <p className="text-sm font-semibold text-foreground">Recent Inspections</p>
              <Button variant="ghost" size="sm" asChild>
                <Link to={ROUTES.history}>
                  View all <ArrowRight className="size-3.5" />
                </Link>
              </Button>
            </div>
            {loadingRecent ? (
              <div className="space-y-2 p-4">
                {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-12 w-full" />)}
              </div>
            ) : !recentInspections || recentInspections.items.length === 0 ? (
              <EmptyState
                icon={ClipboardList}
                title="No evaluated inspections yet"
                description="Run the AI Pipeline on an inspection to see it here."
                className="h-48"
                action={
                  <Button variant="ai" size="sm" asChild>
                    <Link to={ROUTES.newInspection}>
                      <Plus className="size-3.5" /> New Inspection
                    </Link>
                  </Button>
                }
              />
            ) : (
              <ul className="divide-y divide-border">
                {recentInspections.items.map((inspection) => (
                  <li key={inspection.id}>
                    <Link
                      to={ROUTES.workspace(inspection.id)}
                      className="flex items-center justify-between gap-3 px-4 py-3 transition-colors hover:bg-surface-sunken"
                    >
                      <div className="min-w-0">
                        <p className="truncate text-sm font-medium text-foreground">
                          {inspectionDisplayName(inspection)}
                        </p>
                        <div className="mt-0.5 flex items-center gap-2 text-[11px] text-faint-foreground">
                          {inspection.region && (
                            <span className="flex items-center gap-1">
                              <MapPin className="size-3" /> {inspection.region}
                            </span>
                          )}
                          <span>{new Date(inspection.startedAt).toLocaleDateString()}</span>
                        </div>
                      </div>
                      <div className="flex shrink-0 items-center gap-3">
                        {inspection.complianceScore != null && (
                          <span className="tabular text-sm font-semibold" style={{ color: scoreToColor(inspection.complianceScore) }}>
                            {Math.round(inspection.complianceScore)}
                          </span>
                        )}
                        <StatusBadge status={inspection.status} />
                      </div>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </motion.div>
        </div>
      </div>
    </div>
  );
}
