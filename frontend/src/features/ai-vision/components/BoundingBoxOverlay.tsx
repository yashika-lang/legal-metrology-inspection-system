import { cn } from "@/lib/cn";
import type { VisionBoundingBox } from "@/features/inspections/types/vision.types";
import type { Severity } from "@/types/enums";

export interface OverlayBox {
  id: string;
  box: VisionBoundingBox;
  label: string;
  severity?: Severity;
  confidence?: number | null;
}

const SEVERITY_COLOR: Record<Severity, string> = {
  CRITICAL: "var(--color-critical)",
  MAJOR: "var(--color-warning)",
  MINOR: "var(--color-info)",
};

/** Renders a set of image-relative [0,1] boxes over whatever container it's placed inside (must be `position: relative` and match the image's aspect box exactly). */
export function BoundingBoxOverlay({
  boxes,
  selectedId,
  onSelect,
}: {
  boxes: OverlayBox[];
  selectedId?: string | null;
  onSelect?: (id: string) => void;
}) {
  return (
    <div className="pointer-events-none absolute inset-0">
      {boxes.map((item) => {
        const color = item.severity ? SEVERITY_COLOR[item.severity] : "var(--color-accent)";
        const isSelected = item.id === selectedId;
        return (
          <button
            key={item.id}
            type="button"
            onClick={() => onSelect?.(item.id)}
            className={cn(
              "pointer-events-auto absolute rounded-[3px] border-2 transition-all duration-150",
              isSelected ? "z-10 shadow-glow" : "hover:brightness-110",
            )}
            style={{
              left: `${item.box.x * 100}%`,
              top: `${item.box.y * 100}%`,
              width: `${item.box.w * 100}%`,
              height: `${item.box.h * 100}%`,
              borderColor: color,
              backgroundColor: isSelected ? `color-mix(in oklab, ${color} 18%, transparent)` : "transparent",
            }}
          >
            <span
              className="absolute -top-6 left-0 whitespace-nowrap rounded-sm px-1.5 py-0.5 text-[10px] font-medium text-white shadow-sm"
              style={{ backgroundColor: color }}
            >
              {item.label}
              {typeof item.confidence === "number" && ` · ${Math.round(item.confidence * 100)}%`}
            </span>
          </button>
        );
      })}
    </div>
  );
}
