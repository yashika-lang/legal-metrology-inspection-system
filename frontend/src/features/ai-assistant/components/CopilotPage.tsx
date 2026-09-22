import { useState } from "react";
import { Sparkles, ClipboardList } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { EmptyState } from "@/components/common/EmptyState";
import { Skeleton } from "@/components/ui/skeleton";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Sheet } from "@/components/ui/sheet";
import { cn } from "@/lib/cn";
import { useInspections } from "@/features/inspections/hooks/useInspection";
import { inspectionDisplayName } from "@/features/inspections/utils/displayName";
import { ChatPanel } from "./ChatPanel";

/**
 * Standalone entry point for the sidebar's "AI Copilot" destination — the
 * same real Nirikshak drawer already embedded in the Inspection Workspace,
 * just reachable without first opening a specific inspection. Pick an
 * inspection here, and the exact same `ChatPanel` (same hooks, same real
 * backend endpoints) opens for it.
 */
export function CopilotPage() {
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const { data, isLoading } = useInspections({ mine: true, size: 20, sort: "startedAt,desc" });

  return (
    <div className="flex h-full overflow-hidden">
      <div className="flex min-w-0 flex-1 flex-col lg:w-96 lg:flex-none lg:border-r lg:border-border">
        <PageHeader
          title="AI Copilot"
          description="Ask Nirikshak about any of your inspections — explains and recommends, never decides compliance."
        />
        <div className="min-h-0 flex-1 overflow-y-auto">
          {isLoading ? (
            <div className="space-y-2 p-4">{Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-14 w-full" />)}</div>
          ) : !data || data.items.length === 0 ? (
            <EmptyState icon={ClipboardList} title="No inspections yet" description="Start an inspection to ask Nirikshak about it." className="h-full" />
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
      </div>

      <div className="hidden min-h-0 min-w-0 flex-1 items-center justify-center lg:flex">
        <EmptyState
          icon={Sparkles}
          title="Select an inspection"
          description="Choose one on the left to open Nirikshak for it."
        />
      </div>

      <Sheet open={!!selectedId} onOpenChange={(open) => !open && setSelectedId(null)}>
        {selectedId && <ChatPanel inspectionId={selectedId} />}
      </Sheet>
    </div>
  );
}
