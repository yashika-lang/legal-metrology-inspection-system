import { useEffect, useRef, useState } from "react";
import * as DialogPrimitive from "@radix-ui/react-dialog";
import { X, Camera, RotateCcw, Check } from "lucide-react";
import { Button } from "@/components/ui/button";

interface CameraCaptureDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCapture: (file: File) => void;
  /** Shown in the dialog title — lets the Guided 360° Capture flow say which angle it's currently asking for. */
  title?: string;
}

/** Live camera preview → freeze a frame to a canvas → hand back a real File, the same shape `useDropzone` produces, so the upload path never needs to know which acquisition method was used. */
export function CameraCaptureDialog({ open, onOpenChange, onCapture, title = "Capture Label Photo" }: CameraCaptureDialogProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const [snapshot, setSnapshot] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    let cancelled = false;

    navigator.mediaDevices
      .getUserMedia({ video: { facingMode: "environment", width: { ideal: 1920 }, height: { ideal: 1080 } } })
      .then((stream) => {
        if (cancelled) {
          stream.getTracks().forEach((t) => t.stop());
          return;
        }
        streamRef.current = stream;
        if (videoRef.current) videoRef.current.srcObject = stream;
      })
      .catch(() => setError("Could not access the camera — check permissions."));

    return () => {
      cancelled = true;
      streamRef.current?.getTracks().forEach((t) => t.stop());
      streamRef.current = null;
      setSnapshot(null);
      setError(null);
    };
  }, [open]);

  function handleCapture() {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    if (!video || !canvas) return;
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext("2d")?.drawImage(video, 0, 0);
    setSnapshot(canvas.toDataURL("image/png"));
  }

  function handleConfirm() {
    const canvas = canvasRef.current;
    if (!canvas) return;
    canvas.toBlob((blob) => {
      if (!blob) return;
      onCapture(new File([blob], `capture-${Date.now()}.png`, { type: "image/png" }));
      onOpenChange(false);
    }, "image/png");
  }

  return (
    <DialogPrimitive.Root open={open} onOpenChange={onOpenChange}>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className="fixed inset-0 z-40 bg-overlay animate-fade-in" />
        <DialogPrimitive.Content className="fixed left-1/2 top-1/2 z-50 w-full max-w-md -translate-x-1/2 -translate-y-1/2 rounded-lg border border-border bg-surface-raised p-4 shadow-lg animate-scale-in">
          <div className="mb-3 flex items-center justify-between">
            <DialogPrimitive.Title className="flex items-center gap-2 text-sm font-semibold text-foreground">
              <Camera className="size-4 text-accent" />
              {title}
            </DialogPrimitive.Title>
            <DialogPrimitive.Close asChild>
              <Button variant="ghost" size="icon">
                <X className="size-4" />
              </Button>
            </DialogPrimitive.Close>
          </div>

          <div className="relative aspect-[4/3] overflow-hidden rounded-md bg-black">
            {snapshot ? (
              <img src={snapshot} alt="Captured preview" className="size-full object-cover" />
            ) : (
              <video ref={videoRef} autoPlay muted playsInline className="size-full object-cover" />
            )}
            <canvas ref={canvasRef} className="hidden" />
          </div>

          {error && <p className="mt-2 text-center text-xs text-critical">{error}</p>}

          <div className="mt-3 flex justify-center gap-2">
            {snapshot ? (
              <>
                <Button variant="secondary" onClick={() => setSnapshot(null)}>
                  <RotateCcw className="size-4" />
                  Retake
                </Button>
                <Button onClick={handleConfirm}>
                  <Check className="size-4" />
                  Use Photo
                </Button>
              </>
            ) : (
              <Button onClick={handleCapture} disabled={!!error}>
                <Camera className="size-4" />
                Capture
              </Button>
            )}
          </div>
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  );
}
