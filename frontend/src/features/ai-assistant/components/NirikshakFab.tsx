import { Sparkles } from "lucide-react";
import { motion } from "framer-motion";

/**
 * Floating trigger for the Nirikshak Copilot drawer — deliberately separate
 * from the workspace header's action buttons (Run AI Pipeline, etc.) so the
 * one AI-assistant entry point reads as its own persistent affordance,
 * reachable regardless of which panel/tab the officer is looking at.
 */
export function NirikshakFab({ onClick }: { onClick: () => void }) {
  return (
    <motion.button
      type="button"
      onClick={onClick}
      initial={{ opacity: 0, scale: 0.8, y: 12 }}
      animate={{ opacity: 1, scale: 1, y: 0 }}
      transition={{ delay: 0.3, type: "spring", stiffness: 260, damping: 20 }}
      whileHover={{ scale: 1.05 }}
      whileTap={{ scale: 0.96 }}
      className="bg-gradient-ai fixed bottom-6 right-6 z-30 flex items-center gap-2 rounded-full px-4 py-3 text-sm font-semibold text-white shadow-glow-ai animate-glow-breathe"
      aria-label="Open Nirikshak AI Copilot"
    >
      <Sparkles className="size-4" />
      Nirikshak
    </motion.button>
  );
}
