import { AlertTriangle, AlertOctagon, Info } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import type { Severity } from "@/types/enums";

const SEVERITY_CONFIG: Record<Severity, { label: string; variant: "critical" | "warning" | "info"; icon: typeof AlertTriangle }> = {
  CRITICAL: { label: "Critical", variant: "critical", icon: AlertOctagon },
  MAJOR: { label: "Major", variant: "warning", icon: AlertTriangle },
  MINOR: { label: "Minor", variant: "info", icon: Info },
};

export function SeverityBadge({ severity }: { severity: Severity }) {
  const config = SEVERITY_CONFIG[severity];
  const Icon = config.icon;
  return (
    <Badge variant={config.variant}>
      <Icon className="size-3" />
      {config.label}
    </Badge>
  );
}
