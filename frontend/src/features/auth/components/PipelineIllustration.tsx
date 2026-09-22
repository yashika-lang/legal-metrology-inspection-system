import { motion } from "framer-motion";
import { ImageIcon, ScanText, Eye, ScrollText, Fingerprint, FileCheck2 } from "lucide-react";

const STAGES = [
  { icon: ImageIcon, label: "Image" },
  { icon: ScanText, label: "OCR" },
  { icon: Eye, label: "Vision AI" },
  { icon: ScrollText, label: "Rule Engine" },
  { icon: Fingerprint, label: "Evidence" },
  { icon: FileCheck2, label: "Report" },
] as const;

/**
 * The real pipeline this product runs, end to end — the same six stages
 * tracked by the backend's own Decision Trace (`IMAGE_UPLOADED` → ... →
 * `REPORT_GENERATED`), shown here as the first thing anyone sees rather
 * than generic hero art.
 */
export function PipelineIllustration() {
  return (
    <div className="flex flex-wrap items-center gap-x-1 gap-y-6">
      {STAGES.map((stage, index) => (
        <div key={stage.label} className="flex items-center">
          <motion.div
            initial={{ opacity: 0, y: 10, scale: 0.9 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            transition={{ delay: 0.5 + index * 0.1, type: "spring", stiffness: 260, damping: 20 }}
            className="flex flex-col items-center gap-2"
          >
            <div className="bg-gradient-ai relative flex size-12 items-center justify-center rounded-xl text-white shadow-glow-ai">
              <stage.icon className="size-5" />
            </div>
            <span className="text-[11px] font-medium text-white/80">{stage.label}</span>
          </motion.div>

          {index < STAGES.length - 1 && (
            <div className="relative mx-1.5 mb-5 h-px w-6 overflow-hidden bg-white/15 sm:w-9">
              <motion.div
                className="absolute inset-y-0 w-2.5 bg-gradient-ai"
                initial={{ x: "-100%" }}
                animate={{ x: "300%" }}
                transition={{
                  duration: 1.6,
                  repeat: Infinity,
                  ease: "linear",
                  delay: 1.2 + index * 0.15,
                }}
              />
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
