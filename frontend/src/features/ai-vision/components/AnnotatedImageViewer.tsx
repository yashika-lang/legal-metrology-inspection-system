import { useRef, useState, useCallback, type WheelEvent, type MouseEvent } from "react";
import { ZoomIn, ZoomOut, Maximize2, Eye, EyeOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import { BoundingBoxOverlay, type OverlayBox } from "./BoundingBoxOverlay";
import { cn } from "@/lib/cn";

const MIN_ZOOM = 1;
const MAX_ZOOM = 4;

interface AnnotatedImageViewerProps {
  imageUrl: string;
  boxes: OverlayBox[];
  selectedBoxId?: string | null;
  onSelectBox?: (id: string) => void;
}

export function AnnotatedImageViewer({ imageUrl, boxes, selectedBoxId, onSelectBox }: AnnotatedImageViewerProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const [showBoxes, setShowBoxes] = useState(true);
  const dragStart = useRef({ x: 0, y: 0, panX: 0, panY: 0 });

  const clampZoom = (z: number) => Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, z));

  const handleWheel = useCallback((e: WheelEvent) => {
    e.preventDefault();
    setZoom((prev) => clampZoom(prev - e.deltaY * 0.0015));
  }, []);

  const handleMouseDown = (e: MouseEvent) => {
    if (zoom <= 1) return;
    setIsDragging(true);
    dragStart.current = { x: e.clientX, y: e.clientY, panX: pan.x, panY: pan.y };
  };

  const handleMouseMove = (e: MouseEvent) => {
    if (!isDragging) return;
    const dx = e.clientX - dragStart.current.x;
    const dy = e.clientY - dragStart.current.y;
    setPan({ x: dragStart.current.panX + dx, y: dragStart.current.panY + dy });
  };

  const stopDragging = () => setIsDragging(false);

  const resetView = () => {
    setZoom(1);
    setPan({ x: 0, y: 0 });
  };

  return (
    <div className="relative flex h-full flex-col bg-surface-sunken">
      <div
        ref={containerRef}
        onWheel={handleWheel}
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={stopDragging}
        onMouseLeave={stopDragging}
        className={cn(
          "relative flex-1 overflow-hidden",
          zoom > 1 && (isDragging ? "cursor-grabbing" : "cursor-grab"),
        )}
      >
        <div
          className="flex size-full items-center justify-center transition-transform duration-100 ease-out"
          style={{ transform: `translate(${pan.x}px, ${pan.y}px) scale(${zoom})` }}
        >
          <div className="relative max-h-full max-w-full">
            <img
              src={imageUrl}
              alt="Annotated inspection capture"
              draggable={false}
              className="max-h-[calc(100vh-14rem)] w-auto select-none rounded-sm shadow-md"
            />
            {showBoxes && <BoundingBoxOverlay boxes={boxes} selectedId={selectedBoxId} onSelect={onSelectBox} />}
          </div>
        </div>
      </div>

      <div className="absolute bottom-3 left-1/2 flex -translate-x-1/2 items-center gap-1 rounded-lg border border-border bg-surface-raised/90 p-1 shadow-md backdrop-blur-sm">
        <Button variant="ghost" size="icon" onClick={() => setZoom((z) => clampZoom(z - 0.25))} aria-label="Zoom out">
          <ZoomOut className="size-4" />
        </Button>
        <span className="tabular w-10 text-center text-xs text-muted-foreground">{Math.round(zoom * 100)}%</span>
        <Button variant="ghost" size="icon" onClick={() => setZoom((z) => clampZoom(z + 0.25))} aria-label="Zoom in">
          <ZoomIn className="size-4" />
        </Button>
        <div className="mx-1 h-5 w-px bg-border" />
        <Button variant="ghost" size="icon" onClick={resetView} aria-label="Reset view">
          <Maximize2 className="size-4" />
        </Button>
        <Button variant="ghost" size="icon" onClick={() => setShowBoxes((v) => !v)} aria-label="Toggle overlays">
          {showBoxes ? <Eye className="size-4" /> : <EyeOff className="size-4" />}
        </Button>
      </div>
    </div>
  );
}
