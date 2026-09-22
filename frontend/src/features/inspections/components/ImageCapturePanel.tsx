import { useCallback, useMemo, useRef, useState } from "react";
import { useDropzone } from "react-dropzone";
import { motion } from "framer-motion";
import { UploadCloud, ScanBarcode, QrCode, ImageOff, AlertTriangle, CheckCircle2, XCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { ScannerDialog } from "@/features/scans/components/ScannerDialog";
import { useRecordScan } from "@/features/scans/hooks/useScans";
import { decodeBarcodeFromFile } from "@/features/scans/utils/decodeBarcodeFromFile";
import { GuidedCaptureGrid } from "./GuidedCaptureGrid";
import { ImageQualityCard } from "./ImageQualityCard";
import { useInspectionImages, useUploadImage } from "../hooks/useImages";
import { compressImageForUpload } from "../utils/compressImage";
import { GUIDED_CAPTURE_ANGLES } from "@/types/enums";
import type { ImageType } from "@/types/enums";
import { cn } from "@/lib/cn";
import type { ImageResponse } from "../types";

interface ImageCapturePanelProps {
  inspectionId: string;
  selectedImageId: string | null;
  onSelectImage: (id: string) => void;
  /** Barcode-first workflow: fires once, right after a scan matches a known product — the workspace uses this to auto-run the fast-path pipeline with no button press. */
  onProductMatched?: () => void;
}

export function ImageCapturePanel({ inspectionId, selectedImageId, onSelectImage, onProductMatched }: ImageCapturePanelProps) {
  const { data: images, isLoading } = useInspectionImages(inspectionId);
  const uploadImage = useUploadImage(inspectionId);
  const recordScan = useRecordScan(inspectionId);
  const [scannerMode, setScannerMode] = useState<"barcode" | "qr" | null>(null);

  const activeImage = useMemo(
    () => images?.find((img) => img.id === selectedImageId) ?? images?.[images.length - 1] ?? null,
    [images, selectedImageId],
  );

  /** Extra photos not part of the 6-angle guided flow — shown separately so the guided grid and this gallery never display the same image twice. */
  const extraImages = useMemo(
    () => images?.filter((img) => !GUIDED_CAPTURE_ANGLES.includes(img.imageType)) ?? [],
    [images],
  );

  /**
   * Every upload — whether from a single guided-angle slot, the bulk
   * multi-select picker, or a multi-file drag-and-drop — goes through this
   * one queue instead of firing in parallel. Found live: selecting several
   * photos at once fired all of their multipart uploads to Supabase
   * Storage simultaneously, and on this machine's home upload bandwidth
   * that contention alone was enough to blow past the storage timeout —
   * uploads silently failed with no clear signal in the UI. Running them
   * one at a time removes the contention entirely and keeps the shared
   * `uploadImage` mutation's pending/error state meaningful (it reflects
   * exactly one in-flight upload at a time instead of racing).
   */
  const uploadQueueRef = useRef<Promise<unknown>>(Promise.resolve());
  /** Guards against triggering the fast pipeline more than once if several uploaded photos each happen to have a readable barcode (e.g. both a Back photo and a barcode close-up). */
  const hasAutoMatchedRef = useRef(false);
  const [queueProgress, setQueueProgress] = useState<{ done: number; total: number } | null>(null);
  const enqueueUpload = useCallback(
    (file: File, imageType: ImageType) => {
      setQueueProgress((prev) => ({ done: prev?.done ?? 0, total: (prev?.total ?? 0) + 1 }));
      const next = uploadQueueRef.current
        .catch(() => undefined)
        .then(async () => {
          const compressed = await compressImageForUpload(file);
          const uploaded = await uploadImage.mutateAsync({ file: compressed, imageType });

          /*
           * Best-effort, non-blocking: an inspector shouldn't have to
           * separately open the barcode scanner and re-point the camera
           * at a barcode that's already plainly visible in a photo they
           * just took (a Back photo especially). Never awaited — a slow
           * or unsuccessful decode must never delay the upload itself.
           */
          if (!hasAutoMatchedRef.current) {
            decodeBarcodeFromFile(file).then((value) => {
              if (!value || hasAutoMatchedRef.current) return;
              recordScan.mutate(
                { scanType: "BARCODE", scanValue: value },
                {
                  onSuccess: (result) => {
                    if (result.matchedProductId && !hasAutoMatchedRef.current) {
                      hasAutoMatchedRef.current = true;
                      onProductMatched?.();
                    }
                  },
                },
              );
            });
          }

          return uploaded;
        })
        .finally(() => {
          setQueueProgress((prev) => {
            if (!prev) return null;
            const done = prev.done + 1;
            return done >= prev.total ? null : { done, total: prev.total };
          });
        });
      // Keep the chain itself always-resolved so a failed upload doesn't
      // break subsequent queued uploads or surface as an unhandled
      // rejection — the failure is still visible via uploadImage.isError.
      uploadQueueRef.current = next.catch(() => undefined);
    },
    [uploadImage, recordScan, onProductMatched],
  );

  const onDrop = useCallback(
    (accepted: File[]) => {
      accepted.forEach((file) => enqueueUpload(file, "OTHER"));
    },
    [enqueueUpload],
  );

  function handleDetect(value: string) {
    recordScan.mutate(
      { scanType: scannerMode === "barcode" ? "BARCODE" : "QR", scanValue: value },
      {
        onSuccess: (result) => {
          if (result.matchedProductId) {
            hasAutoMatchedRef.current = true;
            onProductMatched?.();
          }
        },
      },
    );
  }

  function handleGuidedUpload(file: File, imageType: ImageType) {
    enqueueUpload(file, imageType);
  }

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: { "image/jpeg": [], "image/png": [], "image/webp": [], "image/heic": [] },
    maxSize: 15 * 1024 * 1024,
  });

  return (
    <div className="flex h-full flex-col gap-3 overflow-y-auto p-3 pb-[max(0.75rem,env(safe-area-inset-bottom))]">
      <GuidedCaptureGrid images={images ?? []} onUpload={handleGuidedUpload} />

      <div className="grid grid-cols-2 gap-2">
        <Button variant="secondary" size="sm" className="flex-col gap-1 py-3 h-auto" onClick={() => setScannerMode("barcode")}>
          <ScanBarcode className="size-4" />
          <span className="text-[10px]">Barcode</span>
        </Button>
        <Button variant="secondary" size="sm" className="flex-col gap-1 py-3 h-auto" onClick={() => setScannerMode("qr")}>
          <QrCode className="size-4" />
          <span className="text-[10px]">QR Code</span>
        </Button>
      </div>

      {recordScan.isPending && (
        <Badge variant="accent" className="justify-center py-1.5">
          Looking up scan…
        </Badge>
      )}
      {recordScan.isSuccess && (
        <Badge
          variant={recordScan.data.matchedProductName ? "success" : "outline"}
          className="justify-center py-1.5"
        >
          {recordScan.data.matchedProductName ? (
            <>
              <CheckCircle2 className="size-3" /> Matched: {recordScan.data.matchedProductName}
            </>
          ) : (
            <>
              <XCircle className="size-3" /> Scanned {recordScan.data.scanValue} — no product match
            </>
          )}
        </Badge>
      )}

      <div
        {...getRootProps()}
        className={cn(
          "flex cursor-pointer flex-col items-center justify-center gap-1.5 rounded-lg border-2 border-dashed px-4 py-5 text-center transition-colors",
          isDragActive ? "border-accent bg-accent-soft" : "border-border-strong hover:border-accent/50 hover:bg-surface-sunken",
        )}
      >
        <input {...getInputProps()} />
        <UploadCloud className="size-5 text-muted-foreground" />
        <p className="text-[11px] font-medium text-foreground">Additional photos (barcode close-up, defects, etc.)</p>
      </div>

      {queueProgress && (
        <p className="text-center text-[11px] text-muted-foreground">
          {queueProgress.total > 1 ? (
            <>
              {queueProgress.done > 0 && (
                <span className="text-success">
                  {queueProgress.done} of {queueProgress.total} photo{queueProgress.done > 1 ? "s" : ""} uploaded —{" "}
                </span>
              )}
              Uploading photo {queueProgress.done + 1} of {queueProgress.total}…
            </>
          ) : (
            "Uploading…"
          )}
        </p>
      )}
      {uploadImage.isError && !queueProgress && (
        <p className="text-center text-[11px] text-critical">
          {(uploadImage.error as Error)?.message || "Upload failed. Please try again."}
        </p>
      )}

      {isLoading && (
        <div className="grid grid-cols-2 gap-2">
          <Skeleton className="aspect-square" />
          <Skeleton className="aspect-square" />
        </div>
      )}

      {!isLoading && extraImages.length === 0 && (
        <EmptyState icon={ImageOff} title="No additional photos" className="py-4" />
      )}

      {extraImages.length > 0 && (
        <div className="grid grid-cols-2 gap-2">
          {extraImages.map((image, index) => (
            <motion.div
              key={image.id}
              initial={{ opacity: 0, y: 6 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.04 }}
            >
              <ImageThumbnail
                image={image}
                selected={image.id === selectedImageId}
                onClick={() => onSelectImage(image.id)}
              />
            </motion.div>
          ))}
        </div>
      )}

      <ImageQualityCard image={activeImage} />

      {scannerMode && (
        <ScannerDialog
          open={!!scannerMode}
          onOpenChange={(open) => !open && setScannerMode(null)}
          mode={scannerMode}
          onDetect={handleDetect}
        />
      )}
    </div>
  );
}

function ImageThumbnail({ image, selected, onClick }: { image: ImageResponse; selected: boolean; onClick: () => void }) {
  const hasWarning = image.recommendedAction === "RETAKE" || image.recommendedAction === "ENHANCE";
  return (
    <button
      onClick={onClick}
      className={cn(
        "group relative aspect-square overflow-hidden rounded-md border-2 transition-all",
        selected ? "border-accent shadow-glow" : "border-transparent hover:border-border-strong",
      )}
    >
      <img src={image.signedUrl} alt={image.imageType} className="size-full object-cover" />
      <div className="absolute inset-x-0 bottom-0 flex items-center justify-between bg-gradient-to-t from-black/70 to-transparent px-1.5 py-1">
        <span className="text-[9px] font-medium uppercase text-white/90">{image.imageType}</span>
        {hasWarning && <AlertTriangle className="size-3 text-warning" />}
      </div>
    </button>
  );
}
