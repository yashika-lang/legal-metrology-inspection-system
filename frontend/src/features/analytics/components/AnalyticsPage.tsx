import { useState } from "react";
import {
  ResponsiveContainer,
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
} from "recharts";
import { TrendingUp, Trophy, ShieldAlert, Lightbulb, ChevronDown, ChevronUp } from "lucide-react";
import { motion } from "framer-motion";
import { PageHeader } from "@/components/common/PageHeader";
import { EmptyState } from "@/components/common/EmptyState";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from "@/components/ui/select";
import { cn } from "@/lib/cn";
import {
  useTimeseries,
  useRegionSeverityHeatmap,
  useLeaderboard,
  useRiskIndex,
  useInsights,
  useForecast,
  useAnomalies,
} from "../hooks/useAnalytics";
import type { RiskBand } from "../types/analytics.types";

const RISK_BAND_VARIANT: Record<RiskBand, "success" | "warning" | "critical"> = {
  LOW: "success",
  MEDIUM: "warning",
  HIGH: "critical",
};

export function AnalyticsPage() {
  const [tab, setTab] = useState("trends");

  return (
    <div className="h-full overflow-y-auto">
      <PageHeader title="Analytics" description="Trends, leaderboards, risk scoring, and AI-generated insights — computed live from real inspection data." />

      <div className="p-4 sm:p-6">
        <Tabs value={tab} onValueChange={setTab}>
          <TabsList>
            <TabsTrigger value="trends"><TrendingUp className="size-3.5" /> Trends</TabsTrigger>
            <TabsTrigger value="leaderboards"><Trophy className="size-3.5" /> Leaderboards</TabsTrigger>
            <TabsTrigger value="risk"><ShieldAlert className="size-3.5" /> Risk Index</TabsTrigger>
            <TabsTrigger value="insights"><Lightbulb className="size-3.5" /> Insights</TabsTrigger>
          </TabsList>

          <TabsContent value="trends" className="mt-5"><TrendsTab /></TabsContent>
          <TabsContent value="leaderboards" className="mt-5"><LeaderboardsTab /></TabsContent>
          <TabsContent value="risk" className="mt-5"><RiskTab /></TabsContent>
          <TabsContent value="insights" className="mt-5"><InsightsTab /></TabsContent>
        </Tabs>
      </div>
    </div>
  );
}

function ChartCard({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="rounded-xl border border-border bg-surface-raised p-4">
      <p className="mb-3 text-sm font-semibold text-foreground">{title}</p>
      {children}
    </div>
  );
}

function TrendsTab() {
  const { data: violations, isLoading: l1 } = useTimeseries("violations", 12);
  const { data: inspections, isLoading: l2 } = useTimeseries("inspections", 12);
  const { data: heatmap, isLoading: l3 } = useRegionSeverityHeatmap();

  const regions = Array.from(new Set(heatmap?.map((h) => h.region) ?? []));
  const severities = ["CRITICAL", "MAJOR", "MINOR"];
  const cellCount = (region: string, severity: string) =>
    heatmap?.find((h) => h.region === region && h.severity === severity)?.count ?? 0;
  const maxCount = Math.max(1, ...(heatmap?.map((h) => h.count) ?? [0]));

  return (
    <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
      <ChartCard title="Violations — Last 12 Months">
        {l1 ? <Skeleton className="h-56 w-full" /> : (
          <ResponsiveContainer width="100%" height={224}>
            <BarChart data={violations}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" vertical={false} />
              <XAxis dataKey="month" tick={{ fontSize: 10, fill: "var(--color-faint-foreground)" }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 11, fill: "var(--color-faint-foreground)" }} axisLine={false} tickLine={false} width={24} />
              <Tooltip contentStyle={{ background: "var(--color-surface-raised)", border: "1px solid var(--color-border)", borderRadius: 8, fontSize: 12 }} />
              <Bar dataKey="count" fill="var(--color-warning)" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        )}
      </ChartCard>

      <ChartCard title="Inspections — Last 12 Months">
        {l2 ? <Skeleton className="h-56 w-full" /> : (
          <ResponsiveContainer width="100%" height={224}>
            <LineChart data={inspections}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" vertical={false} />
              <XAxis dataKey="month" tick={{ fontSize: 10, fill: "var(--color-faint-foreground)" }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 11, fill: "var(--color-faint-foreground)" }} axisLine={false} tickLine={false} width={24} />
              <Tooltip contentStyle={{ background: "var(--color-surface-raised)", border: "1px solid var(--color-border)", borderRadius: 8, fontSize: 12 }} />
              <Line type="monotone" dataKey="count" stroke="var(--color-ai-blue)" strokeWidth={2} dot={{ r: 3 }} />
            </LineChart>
          </ResponsiveContainer>
        )}
      </ChartCard>

      <ChartCard title="Region × Severity Heatmap">
        {l3 ? (
          <Skeleton className="h-40 w-full" />
        ) : !heatmap || heatmap.length === 0 ? (
          <EmptyState icon={ShieldAlert} title="No data yet" className="h-40" />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[420px] border-separate border-spacing-1 text-xs">
              <thead>
                <tr>
                  <th className="text-left font-medium text-muted-foreground">Region</th>
                  {severities.map((s) => <th key={s} className="px-2 font-medium text-muted-foreground">{s}</th>)}
                </tr>
              </thead>
              <tbody>
                {regions.map((region) => (
                  <tr key={region}>
                    <td className="pr-2 font-medium text-foreground">{region}</td>
                    {severities.map((severity) => {
                      const count = cellCount(region, severity);
                      const intensity = count / maxCount;
                      return (
                        <td key={severity} className="p-0">
                          <div
                            className="tabular flex h-9 items-center justify-center rounded-md font-medium text-white"
                            style={{
                              backgroundColor:
                                count === 0
                                  ? "var(--color-surface-sunken)"
                                  : `color-mix(in oklab, var(--color-critical) ${20 + intensity * 70}%, var(--color-surface-sunken))`,
                              color: count === 0 ? "var(--color-faint-foreground)" : undefined,
                            }}
                          >
                            {count}
                          </div>
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </ChartCard>
    </div>
  );
}

function LeaderboardsTab() {
  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
      <LeaderboardCard title="Top Violated Rules" kind="rules" />
      <LeaderboardCard title="Manufacturers by Violations" kind="manufacturers" />
      <LeaderboardCard title="Inspectors by Volume" kind="inspectors" />
    </div>
  );
}

function LeaderboardCard({ title, kind }: { title: string; kind: "rules" | "manufacturers" | "inspectors" }) {
  const { data, isLoading } = useLeaderboard(kind);
  const max = Math.max(1, ...(data?.map((d) => d.count) ?? [0]));

  return (
    <div className="rounded-xl border border-border bg-surface-raised p-4">
      <p className="mb-3 text-sm font-semibold text-foreground">{title}</p>
      {isLoading ? (
        <div className="space-y-2">{Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-6 w-full" />)}</div>
      ) : !data || data.length === 0 ? (
        <EmptyState icon={Trophy} title="No data yet" className="h-32" />
      ) : (
        <ul className="space-y-2.5">
          {data.slice(0, 8).map((item, index) => (
            <li key={`${item.entityId}-${item.label}-${index}`}>
              <div className="mb-1 flex items-center justify-between text-xs">
                <span className="truncate text-foreground-secondary">{index + 1}. {item.label}</span>
                <span className="tabular font-semibold text-foreground">{item.count}</span>
              </div>
              <div className="h-1.5 w-full overflow-hidden rounded-full bg-surface-sunken">
                <motion.div
                  className="bg-gradient-ai h-full rounded-full"
                  initial={{ width: 0 }}
                  animate={{ width: `${(item.count / max) * 100}%` }}
                  transition={{ duration: 0.5 }}
                />
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function RiskTab() {
  const [entity, setEntity] = useState<"manufacturers" | "categories" | "regions" | "inspectors">("manufacturers");
  const { data, isLoading } = useRiskIndex(entity);
  const [expanded, setExpanded] = useState<number | null>(null);

  return (
    <div className="space-y-4">
      <Select value={entity} onValueChange={(v) => setEntity(v as typeof entity)}>
        <SelectTrigger className="w-56"><SelectValue /></SelectTrigger>
        <SelectContent>
          <SelectItem value="manufacturers">Manufacturers</SelectItem>
          <SelectItem value="categories">Categories</SelectItem>
          <SelectItem value="regions">Regions</SelectItem>
          <SelectItem value="inspectors">Inspectors</SelectItem>
        </SelectContent>
      </Select>

      {isLoading ? (
        <div className="space-y-2">{Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-16 w-full" />)}</div>
      ) : !data || data.length === 0 ? (
        <EmptyState icon={ShieldAlert} title="No risk scores computed yet" description="Risk is computed from persisted violations across completed inspections." />
      ) : (
        <ul className="space-y-2">
          {data.map((score, index) => (
            <li key={`${score.entityId}-${index}`} className="rounded-lg border border-border bg-surface-raised">
              <button
                onClick={() => setExpanded(expanded === index ? null : index)}
                className="flex w-full items-center justify-between gap-3 p-3.5 text-left"
              >
                <div>
                  <p className="text-sm font-medium text-foreground">{score.label}</p>
                  <p className="text-[11px] text-faint-foreground">{score.explainability.methodology}</p>
                </div>
                <div className="flex items-center gap-2">
                  <span className="tabular text-lg font-semibold text-foreground">{Math.round(score.riskScore)}</span>
                  <Badge variant={RISK_BAND_VARIANT[score.riskBand]}>{score.riskBand}</Badge>
                  {expanded === index ? <ChevronUp className="size-4 text-muted-foreground" /> : <ChevronDown className="size-4 text-muted-foreground" />}
                </div>
              </button>
              {expanded === index && (
                <div className="space-y-2 border-t border-border p-3.5 text-xs text-foreground-secondary">
                  <p>Confidence: <span className="tabular font-medium">{Math.round(score.explainability.confidence * 100)}%</span></p>
                  <ul className="list-inside list-disc space-y-1">
                    {score.explainability.reasoning.map((r, i) => <li key={i}>{r}</li>)}
                  </ul>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function InsightsTab() {
  const { data: insights, isLoading: l1 } = useInsights();
  const { data: forecasts, isLoading: l2 } = useForecast();
  const { data: anomalies, isLoading: l3 } = useAnomalies();

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <div className="rounded-xl border border-border bg-surface-raised p-4">
        <p className="mb-3 text-sm font-semibold text-foreground">Insights</p>
        {l1 ? <Skeleton className="h-32 w-full" /> : !insights || insights.length === 0 ? (
          <EmptyState icon={Lightbulb} title="No insights yet" description="Insights surface once enough inspection history exists." className="h-32" />
        ) : (
          <ul className="space-y-2.5">
            {insights.map((insight) => (
              <li key={insight.id} className="rounded-lg bg-surface-sunken p-3">
                <div className="mb-1 flex items-center justify-between">
                  <p className="text-sm font-medium text-foreground">{insight.title}</p>
                  <Badge variant="accent">{insight.type.replace(/_/g, " ")}</Badge>
                </div>
                <p className="text-xs text-foreground-secondary">{insight.message}</p>
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="rounded-xl border border-border bg-surface-raised p-4">
        <p className="mb-3 text-sm font-semibold text-foreground">Forecast</p>
        {l2 ? <Skeleton className="h-32 w-full" /> : !forecasts || forecasts.length === 0 ? (
          <EmptyState icon={TrendingUp} title="Not enough history to forecast" className="h-32" />
        ) : (
          <ul className="space-y-2.5">
            {forecasts.map((f) => (
              <li key={f.metric} className="rounded-lg bg-surface-sunken p-3">
                <p className="text-xs font-medium capitalize text-muted-foreground">{f.metric.replace(/_/g, " ")} — {f.targetPeriod}</p>
                <p className="tabular text-xl font-semibold text-foreground">{Math.round(f.predictedValue)}</p>
                <p className="text-[11px] text-faint-foreground">
                  {Math.round(f.confidenceInterval.confidenceLevel * 100)}% CI: {Math.round(f.confidenceInterval.lowerBound)}–{Math.round(f.confidenceInterval.upperBound)}
                  {f.regressionEquation && ` · ${f.regressionEquation}`}
                  {f.rSquared != null && ` · R²=${f.rSquared.toFixed(2)}`}
                </p>
                {f.explainability.reasoning.length > 0 && (
                  <p className="mt-1 text-[10px] italic text-faint-foreground">{f.explainability.reasoning[0]}</p>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="rounded-xl border border-border bg-surface-raised p-4 lg:col-span-2">
        <p className="mb-3 text-sm font-semibold text-foreground">Anomalies</p>
        {l3 ? <Skeleton className="h-24 w-full" /> : !anomalies || anomalies.length === 0 ? (
          <EmptyState icon={ShieldAlert} title="No anomalies detected" description="Statistically normal — nothing deviates beyond the z-score threshold." className="h-24" />
        ) : (
          <ul className="space-y-2">
            {anomalies.map((a, i) => (
              <li key={i} className={cn("rounded-lg border-l-4 bg-surface-sunken p-3 text-xs", "border-warning")}>
                <p className="font-medium text-foreground">{a.dimension}: {a.dimensionValue}</p>
                <p className="mt-0.5 text-foreground-secondary">{a.reason}</p>
                <p className="mt-1 tabular text-[11px] text-faint-foreground">
                  Expected {a.expectedValue.toFixed(1)} · Actual {a.actualValue.toFixed(1)} · z={a.zScore.toFixed(2)}
                </p>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
