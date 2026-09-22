import type { LucideIcon } from "lucide-react";
import { Sparkles } from "lucide-react";
import { EmptyState } from "./EmptyState";

/** Placeholder for sidebar destinations not yet built — keeps navigation real without faking a screen that doesn't exist yet. */
export function ComingSoonPage({ title, icon = Sparkles }: { title: string; icon?: LucideIcon }) {
  return (
    <EmptyState
      icon={icon}
      title={title}
      description="This screen is next on the roadmap — the AI Inspection Workspace was built first as the design reference for the rest of the app."
      className="h-full"
    />
  );
}
