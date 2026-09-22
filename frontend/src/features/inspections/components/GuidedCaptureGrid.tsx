import { useRef, useState } from "react";
import { motion } from "framer-motion";
import { Camera, Upload, CheckCircle2, RotateCcw, AlertTriangle, Images } from "lucide-react";
import {
  ArrowUp,
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  Square,
  RectangleHorizontal,
} from "lucide-react";
import { GUIDED_CAPTURE_ANGLES, IMAGE_TYPE_LABEL } from "@/types/enums";
import type { ImageType } from "@/types/enums";
import { CameraCaptureDialog } from "./CameraCaptureDialog";
import type { ImageResponse } from "../types";

/** Front/Back require the widest, most information-dense faces of a package — the two panels Legal Metrology rules actually target (MRP placement is explicitly checked as "FRONT"). Left/Right/Top/Bottom are strongly recommended for full 360° coverage but not required to unblock analysis. */
export const MINIMUM_REQUIRED_ANGLES: ImageType[] = ["FRONT", "BACK"];

const ANGLE_ICON: Record<ImageType, typeof Square> = {
  FRONT: RectangleHorizontal,
  BACK: RectangleHorizontal,
  LEFT: ArrowLeft,
  RIGHT: ArrowRight,
  TOP: ArrowUp,
  BOTTOM: ArrowDown,
  SIDE: Square,
  OTHER: Square,
};

interface GuidedCaptureGridProps {
  images: ImageResponse[];
  onUpload: (file: File, imageType: ImageType) => void;
}

export function GuidedCaptureGrid({ images, onUpload }: GuidedCaptureGridProps) {
  const [cameraAngle, setCameraAngle] = useState<ImageType | null>(null);
  const fileInputRefs = useRef<Partial<Record<ImageType, HTMLInputElement | null>>>({});
  const bulkInputRef = useRef<HTMLInputElement | null>(null);

  const imageForAngle = (angle: ImageType) => images.find((img) => img.imageType === angle);
  const capturedCount = GUIDED_CAPTURE_ANGLES.filter((a) => imageForAngle(a)).length;
  const requiredMet = MINIMUM_REQUIRED_ANGLES.every((a) => imageForAngle(a));

  function handleFilePick(angle: ImageType, e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) onUpload(file, angle);
    e.target.value = "";
  }

  /**
   * Lets an inspector pick several photos from their gallery in one go
   * instead of filling each of the 6 angle slots individually. Files are
   * assigned to whichever angle slots are still empty, in the fixed
   * GUIDED_CAPTURE_ANGLES order (Front, Back, Left, Right, Top, Bottom);
   * any files beyond the number of empty slots upload as extra/"OTHER"
   * photos (the same bucket as the manual "Additional photos" dropzone),
   * so nothing selected is ever dropped on the floor.
   */
  function handleBulkFilePick(e: React.ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files ?? []);
    const emptyAngles = GUIDED_CAPTURE_ANGLES.filter((a) => !imageForAngle(a));
    files.forEach((file, index) => {
      onUpload(file, emptyAngles[index] ?? "OTHER");
    });
    e.target.value = "";
  }

  return (
    <div className="space-y-2.5">
      <div className="flex items-center justify-between">
        <p className="text-xs font-medium text-muted-foreground">Guided 360° Capture</p>
        <span className="tabular text-[11px] text-faint-foreground">{capturedCount} / 6 angles</span>
      </div>

      <div className="h-1.5 w-full overflow-hidden rounded-full bg-surface-sunken">
        <motion.div
          className="bg-gradient-ai h-full rounded-full"
          initial={{ width: 0 }}
          animate={{ width: `${(capturedCount / 6) * 100}%` }}
          transition={{ duration: 0.4 }}
        />
      </div>

      {capturedCount < 6 && (
        <button
          onClick={() => bulkInputRef.current?.click()}
          className="flex w-full items-center justify-center gap-1.5 rounded-md border border-dashed border-border-strong py-1.5 text-[11px] font-medium text-muted-foreground transition-colors hover:border-accent/50 hover:bg-surface-sunken hover:text-foreground"
        >
          <Images className="size-3.5" />
          Select multiple photos at once
        </button>
      )}
      <input
        ref={bulkInputRef}
        type="file"
        accept="image/*"
        multiple
        className="hidden"
        onChange={handleBulkFilePick}
      />

      <div className="grid grid-cols-3 gap-2">
        {GUIDED_CAPTURE_ANGLES.map((angle) => {
          const image = imageForAngle(angle);
          const Icon = ANGLE_ICON[angle];
          const required = MINIMUM_REQUIRED_ANGLES.includes(angle);

          return (
            <div key={angle} className="flex flex-col items-center gap-1">
              <div className="relative aspect-square w-full overflow-hidden rounded-lg border-2 border-dashed border-border-strong">
                {image ? (
                  <>
                    <img src={image.signedUrl} alt={angle} className="size-full object-cover" />
                    <div className="absolute inset-0 flex items-center justify-center bg-black/0 opacity-0 transition-opacity hover:bg-black/50 hover:opacity-100">
                      <button
                        onClick={() => setCameraAngle(angle)}
                        className="flex items-center gap-1 rounded-full bg-white/90 px-2 py-1 text-[10px] font-medium text-foreground"
                      >
                        <RotateCcw className="size-3" /> Retake
                      </button>
                    </div>
                    <CheckCircle2 className="absolute right-1 top-1 size-4 rounded-full bg-white text-success" />
                  </>
                ) : (
                  <div className="flex size-full flex-col items-center justify-center gap-1 bg-surface-sunken">
                    <Icon className="size-4 text-faint-foreground" />
                    <div className="flex gap-1">
                      <button
                        onClick={() => setCameraAngle(angle)}
                        className="rounded-md p-1 text-muted-foreground hover:bg-surface-raised hover:text-foreground"
                        aria-label={`Capture ${angle} with camera`}
                      >
                        <Camera className="size-3.5" />
                      </button>
                      <button
                        onClick={() => fileInputRefs.current[angle]?.click()}
                        className="rounded-md p-1 text-muted-foreground hover:bg-surface-raised hover:text-foreground"
                        aria-label={`Upload ${angle} photo`}
                      >
                        <Upload className="size-3.5" />
                      </button>
                    </div>
                    <input
                      ref={(el) => {
                        fileInputRefs.current[angle] = el;
                      }}
                      type="file"
                      accept="image/*"
                      className="hidden"
                      onChange={(e) => handleFilePick(angle, e)}
                    />
                  </div>
                )}
              </div>
              <span className="flex items-center gap-1 text-[10px] text-muted-foreground">
                {IMAGE_TYPE_LABEL[angle]}
                {required && !image && <span className="text-critical">*</span>}
              </span>
            </div>
          );
        })}
      </div>

      {!requiredMet && (
        <p className="flex items-center gap-1.5 text-[11px] text-faint-foreground">
          <AlertTriangle className="size-3 text-warning" />
          Front and Back (*) are required before the AI Pipeline can run.
        </p>
      )}

      {cameraAngle && (
        <CameraCaptureDialog
          open={!!cameraAngle}
          onOpenChange={(open) => !open && setCameraAngle(null)}
          title={`Capture: ${IMAGE_TYPE_LABEL[cameraAngle]}`}
          onCapture={(file) => {
            onUpload(file, cameraAngle);
            setCameraAngle(null);
          }}
        />
      )}
    </div>
  );
}
