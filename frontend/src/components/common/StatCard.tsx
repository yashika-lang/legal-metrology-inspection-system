import type { LucideIcon } from "lucide-react";
import { motion } from "framer-motion";
import { cn } from "@/lib/cn";

export function StatCard({
  icon: Icon,
  label,
  value,
  suffix,
  tone = "neutral",
  delay = 0,
}: {
  icon: LucideIcon;
  label: string;
  value: string | number;
  suffix?: string;
  tone?: "neutral" | "accent" | "success" | "warning" | "critical";
  delay?: number;
}) {
  const toneClasses: Record<typeof tone, string> = {
    neutral: "bg-surface-sunken text-foreground-secondary",
    accent: "bg-accent-soft text-accent-soft-foreground",
    success: "bg-success-soft text-success-foreground",
    warning: "bg-warning-soft text-warning-foreground",
    critical: "bg-critical-soft text-critical-foreground",
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay }}
      className="card-hover rounded-xl border border-border bg-surface-raised p-4"
    >
      <div className="flex items-center gap-2">
        <div className={cn("flex size-8 items-center justify-center rounded-lg", toneClasses[tone])}>
          <Icon className="size-4" />
        </div>
        <p className="text-xs font-medium text-muted-foreground">{label}</p>
      </div>
      <p className="tabular mt-3 text-2xl font-semibold tracking-tight text-foreground">
        {value}
        {suffix && <span className="ml-1 text-sm font-normal text-faint-foreground">{suffix}</span>}
      </p>
    </motion.div>
  );
}
