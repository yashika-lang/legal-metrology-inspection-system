import { RadialBarChart, RadialBar, PolarAngleAxis } from "recharts";
import { cn } from "@/lib/cn";

interface ComplianceGaugeProps {
  score: number | null;
  size?: number;
  label?: string;
  className?: string;
}

/** score → color, shared logic so the gauge and any badge referencing the same score never disagree. */
export function scoreToColor(score: number | null | undefined): string {
  if (score == null) return "var(--color-faint-foreground)";
  if (score >= 80) return "var(--color-success)";
  if (score >= 50) return "var(--color-warning)";
  return "var(--color-critical)";
}

export function ComplianceGauge({ score, size = 128, label = "Compliance Score", className }: ComplianceGaugeProps) {
  const value = score ?? 0;
  const color = scoreToColor(score);
  const data = [{ value }];

  return (
    <div className={cn("flex flex-col items-center gap-1", className)}>
      <div style={{ width: size, height: size }} className="relative">
        <RadialBarChart
          width={size}
          height={size}
          cx="50%"
          cy="50%"
          innerRadius="72%"
          outerRadius="100%"
          barSize={size * 0.09}
          data={data}
          startAngle={90}
          endAngle={-270}
        >
          <PolarAngleAxis type="number" domain={[0, 100]} angleAxisId={0} tick={false} />
          <RadialBar
            background={{ fill: "var(--color-surface-sunken)" }}
            dataKey="value"
            cornerRadius={999}
            fill={color}
            isAnimationActive
            animationDuration={800}
          />
        </RadialBarChart>
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <span className="tabular text-2xl font-semibold text-foreground">
            {score == null ? "—" : Math.round(score)}
          </span>
          {score != null && <span className="text-[10px] text-faint-foreground">/ 100</span>}
        </div>
      </div>
      <span className="text-xs font-medium text-muted-foreground">{label}</span>
    </div>
  );
}
