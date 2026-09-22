import { Badge } from "@/components/ui/badge";
import type { InspectionStatus } from "@/types/enums";

const STATUS_CONFIG: Record<InspectionStatus, { label: string; variant: "neutral" | "info" | "warning" | "success" | "accent" }> = {
  DRAFT: { label: "Draft", variant: "neutral" },
  IN_PROGRESS: { label: "In Progress", variant: "info" },
  PENDING_REVIEW: { label: "Pending Review", variant: "warning" },
  COMPLETED: { label: "Completed", variant: "success" },
  CLOSED: { label: "Closed", variant: "neutral" },
};

export function StatusBadge({ status }: { status: InspectionStatus }) {
  const config = STATUS_CONFIG[status];
  return (
    <Badge variant={config.variant} dot>
      {config.label}
    </Badge>
  );
}
