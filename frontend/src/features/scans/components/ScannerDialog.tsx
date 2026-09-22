import { useCallback, useEffect, useRef, useState } from "react";
import { BrowserMultiFormatReader } from "@zxing/browser";
import { X, ScanLine, SwitchCamera, CheckCircle2 } from "lucide-react";
import * as DialogPrimitive from "@radix-ui/react-dialog";
import { motion, AnimatePresence } from "framer-motion";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/cn";

interface ScannerDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onDetect: (value: string) => void;
  mode: "barcode" | "qr";
}

type FacingMode = "environment" | "user";

/**
 * One `onDetect(value)` callback regardless of symbology — ZXing's
 * `BrowserMultiFormatReader` reads both 1D barcodes and QR codes from the
 * same video stream, so "barcode mode" and "qr mode" only change the
 * dialog's copy, not the decode logic.
 *
 * Uses `decodeFromConstraints` with an explicit `facingMode` instead of
 * `decodeFromVideoDevice(undefined, ...)` — the latter lets the browser
 * pick *any* camera with no preference, which on a phone commonly means
 * the front-facing camera (wrong for scanning a product label). `ideal`
 * (not a hard `exact`) constraint means a desktop webcam with no rear/front
 * concept still works — the browser just uses whatever it has. A manual
 * flip button covers the rare device that still guesses wrong, or a
 * desktop with more than one camera attached.
 *
 * Detection is continuous by construction: ZXing's video decode methods
 * poll every video frame via an internal animation-frame loop until a
 * result is found or the scan is stopped — there's no "single frame"
 * mode to opt out of here.
 */
export function ScannerDialog({ open, onOpenChange, onDetect, mode }: ScannerDialogProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const controlsRef = useRef<ReturnType<BrowserMultiFormatReader["decodeFromConstraints"]> extends Promise<infer C> ? C | null : null>(null);
  const [error, setError] = useState<string | null>(null);
  const [facingMode, setFacingMode] = useState<FacingMode>("environment");
  const [hasMultipleCameras, setHasMultipleCameras] = useState(false);
  const [justDetected, setJustDetected] = useState(false);

  useEffect(() => {
    if (!open) return;
    BrowserMultiFormatReader.listVideoInputDevices()
      .then((devices) => setHasMultipleCameras(devices.length > 1))
      .catch(() => setHasMultipleCameras(false));
  }, [open]);

  useEffect(() => {
    if (!open || !videoRef.current) return;

    let cancelled = false;
    const reader = new BrowserMultiFormatReader();

    reader
      .decodeFromConstraints(
        { video: { facingMode: { ideal: facingMode } } },
        videoRef.current,
        (result, err) => {
          if (result && !cancelled) {
            setJustDetected(true);
            onDetect(result.getText());
            setTimeout(() => {
              if (!cancelled) onOpenChange(false);
            }, 400);
          }
          if (err && err.name !== "NotFoundException") {
            setError("Camera error — check permissions and try again.");
          }
        },
      )
      .then((controls) => {
        if (!cancelled) controlsRef.current = controls;
        else controls.stop();
      })
      .catch(() => setError("Could not access the camera. Check browser camera permissions."));

    return () => {
      cancelled = true;
      controlsRef.current?.stop();
      controlsRef.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, facingMode]);

  const flipCamera = useCallback(() => {
    setError(null);
    setFacingMode((prev) => (prev === "environment" ? "user" : "environment"));
  }, []);

  return (
    <DialogPrimitive.Root open={open} onOpenChange={onOpenChange}>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className="fixed inset-0 z-40 bg-overlay animate-fade-in" />
        <DialogPrimitive.Content className="fixed left-1/2 top-1/2 z-50 w-full max-w-sm -translate-x-1/2 -translate-y-1/2 rounded-lg border border-border bg-surface-raised p-4 shadow-lg animate-scale-in">
          <div className="mb-3 flex items-center justify-between">
            <DialogPrimitive.Title className="flex items-center gap-2 text-sm font-semibold text-foreground">
              <ScanLine className="size-4 text-accent" />
              {mode === "barcode" ? "Scan Barcode" : "Scan QR Code"}
            </DialogPrimitive.Title>
            <div className="flex items-center gap-1">
              {hasMultipleCameras && (
                <Button variant="ghost" size="icon" onClick={flipCamera} aria-label="Switch camera">
                  <SwitchCamera className="size-4" />
                </Button>
              )}
              <DialogPrimitive.Close asChild>
                <Button variant="ghost" size="icon">
                  <X className="size-4" />
                </Button>
              </DialogPrimitive.Close>
            </div>
          </div>

          <div className="relative aspect-square overflow-hidden rounded-md bg-black">
            <video ref={videoRef} className="size-full object-cover" muted playsInline />
            <div
              className={cn(
                "pointer-events-none absolute inset-6 rounded-md border-2 transition-colors",
                justDetected ? "border-success" : "border-accent/70",
              )}
            />
            <AnimatePresence>
              {justDetected && (
                <motion.div
                  initial={{ opacity: 0, scale: 0.8 }}
                  animate={{ opacity: 1, scale: 1 }}
                  className="absolute inset-0 flex items-center justify-center bg-black/40"
                >
                  <div className="flex items-center gap-2 rounded-full bg-success px-4 py-2 text-sm font-medium text-white">
                    <CheckCircle2 className="size-4" /> Detected
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>

          {error ? (
            <p className="mt-3 text-center text-xs text-critical">{error}</p>
          ) : (
            <p className="mt-3 text-center text-xs text-muted-foreground">
              Align the {mode === "barcode" ? "barcode" : "QR code"} within the frame — scanning is continuous, no need to tap anything.
            </p>
          )}
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  );
}
