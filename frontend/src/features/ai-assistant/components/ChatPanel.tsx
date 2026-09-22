import { useState, type FormEvent } from "react";
import {
  Sparkles,
  Send,
  ShieldAlert,
  ScrollText,
  Wrench,
  NotebookPen,
  GitCompareArrows,
  HelpCircle,
  FileText,
  FileWarning,
  Gavel,
  Gauge,
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { Button } from "@/components/ui/button";
import { Input, Textarea } from "@/components/ui/input";
import { ScrollArea } from "@/components/ui/scroll-area";
import { SheetContent, SheetHeader, SheetTitle, SheetDescription } from "@/components/ui/sheet";
import { useCopilotChat } from "../hooks/useCopilotChat";
import { AnswerCard } from "./AnswerCard";
import { cn } from "@/lib/cn";

interface ChatPanelProps {
  inspectionId: string;
  /** The rule code of whichever violation is currently selected on the right panel — "Explain Rule" targets it directly instead of guessing at free text. */
  selectedRuleCode?: string | null;
}

type InlineForm = "notes" | "compare" | null;

export function ChatPanel({ inspectionId, selectedRuleCode }: ChatPanelProps) {
  const {
    turns,
    ask,
    explainRule,
    explainViolations,
    summarize,
    howToFix,
    generateOfficerNotes,
    compareWith,
    generateNotice,
    analyzeRisk,
    isSending,
  } = useCopilotChat(inspectionId);
  const [input, setInput] = useState("");
  const [inlineForm, setInlineForm] = useState<InlineForm>(null);
  const [notesDraft, setNotesDraft] = useState("");
  const [compareId, setCompareId] = useState("");

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const question = input.trim();
    if (!question || isSending) return;
    setInput("");
    void ask(question);
  }

  function submitNotes(e: FormEvent) {
    e.preventDefault();
    if (!notesDraft.trim() || isSending) return;
    void generateOfficerNotes(notesDraft.trim());
    setNotesDraft("");
    setInlineForm(null);
  }

  function submitCompare(e: FormEvent) {
    e.preventDefault();
    if (!compareId.trim() || isSending) return;
    void compareWith(compareId.trim());
    setCompareId("");
    setInlineForm(null);
  }

  return (
    <SheetContent side="right" className="w-full sm:max-w-sm">
      <SheetHeader>
        <SheetTitle className="flex items-center gap-2">
          <Sparkles className="size-4 text-accent" />
          Nirikshak
        </SheetTitle>
        <SheetDescription>Your AI inspection assistant — explains and recommends, never decides compliance.</SheetDescription>
      </SheetHeader>

      {/* Quick action chips — always visible, not just on the empty state, so they stay reachable mid-conversation. All 7 call a real backend endpoint: explainRule/summarize/manufacturerRecommendations/compare have their own dedicated routes; Why Failed/Explain Violations/Generate Notice/Risk Analysis compose the real ask() endpoint with a specific prompt (no dedicated route exists for those four). */}
      <div className="flex flex-wrap gap-1.5 border-b border-border px-4 py-3">
        <ActionChip icon={HelpCircle} label="Why Failed?" onClick={() => void ask("Why did this inspection fail?")} disabled={isSending} />
        <ActionChip
          icon={ScrollText}
          label="Explain Rule"
          disabled={!selectedRuleCode || isSending}
          title={!selectedRuleCode ? "Select a violation on the right panel first" : undefined}
          onClick={() => selectedRuleCode && void explainRule(selectedRuleCode)}
        />
        <ActionChip icon={FileWarning} label="Explain Violations" onClick={() => void explainViolations()} disabled={isSending} />
        <ActionChip icon={FileText} label="Summarize Inspection" onClick={() => void summarize()} disabled={isSending} />
        <ActionChip icon={Wrench} label="Manufacturer Guidance" onClick={() => void howToFix()} disabled={isSending} />
        <ActionChip icon={Gauge} label="Risk Analysis" onClick={() => void analyzeRisk()} disabled={isSending} />
        <ActionChip icon={Gavel} label="Generate Notice" onClick={() => void generateNotice()} disabled={isSending} />
      </div>

      <div className="flex items-center gap-2 border-b border-border px-4 py-2">
        <SuggestedAction icon={NotebookPen} label="Generate officer notes" onClick={() => setInlineForm(inlineForm === "notes" ? null : "notes")} />
        <SuggestedAction icon={GitCompareArrows} label="Compare inspection" onClick={() => setInlineForm(inlineForm === "compare" ? null : "compare")} />
      </div>

      {inlineForm === "notes" && (
        <form onSubmit={submitNotes} className="space-y-2 border-b border-border bg-surface-sunken p-3">
          <Textarea
            autoFocus
            value={notesDraft}
            onChange={(e) => setNotesDraft(e.target.value)}
            placeholder="Jot your rough field observations…"
            className="min-h-16 text-xs"
          />
          <div className="flex justify-end gap-1.5">
            <Button type="button" variant="ghost" size="sm" onClick={() => setInlineForm(null)}>Cancel</Button>
            <Button type="submit" variant="ai" size="sm" disabled={!notesDraft.trim()}>Generate</Button>
          </div>
        </form>
      )}

      {inlineForm === "compare" && (
        <form onSubmit={submitCompare} className="space-y-2 border-b border-border bg-surface-sunken p-3">
          <Input
            autoFocus
            value={compareId}
            onChange={(e) => setCompareId(e.target.value)}
            placeholder="Other inspection ID"
            className="text-xs"
          />
          <div className="flex justify-end gap-1.5">
            <Button type="button" variant="ghost" size="sm" onClick={() => setInlineForm(null)}>Cancel</Button>
            <Button type="submit" variant="ai" size="sm" disabled={!compareId.trim()}>Compare</Button>
          </div>
        </form>
      )}

      <ScrollArea className="flex-1 px-5 py-4">
        {turns.length === 0 ? (
          <div className="flex h-full flex-col items-center justify-center gap-2 text-center">
            <div className="bg-gradient-ai flex size-10 items-center justify-center rounded-full text-white">
              <Sparkles className="size-5" />
            </div>
            <p className="text-sm font-medium text-foreground">Ask Nirikshak anything</p>
            <p className="max-w-52 text-xs text-muted-foreground">
              Use a quick action above, or type your own question below.
            </p>
          </div>
        ) : (
          <div className="flex flex-col gap-4">
            <AnimatePresence initial={false}>
              {turns.map((turn) => (
                <motion.div
                  key={turn.id}
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="flex flex-col gap-2"
                >
                  <div className="ml-8 self-end rounded-lg rounded-tr-sm bg-accent px-3 py-2 text-sm text-accent-foreground">
                    {turn.question}
                  </div>

                  {turn.pending && (
                    <div className="mr-6 flex items-center gap-1.5 rounded-lg rounded-tl-sm bg-surface-sunken px-3 py-2 text-xs text-muted-foreground">
                      <span className="size-1.5 animate-pulse rounded-full bg-accent" />
                      <span className="size-1.5 animate-pulse rounded-full bg-accent [animation-delay:150ms]" />
                      <span className="size-1.5 animate-pulse rounded-full bg-accent [animation-delay:300ms]" />
                    </div>
                  )}

                  {turn.error && (
                    <div className="mr-6 rounded-lg rounded-tl-sm bg-critical-soft px-3 py-2 text-xs text-critical-foreground">
                      {turn.error}
                    </div>
                  )}

                  {turn.answer && <AnswerCard kind={turn.kind} answer={turn.answer} />}
                </motion.div>
              ))}
            </AnimatePresence>
          </div>
        )}
      </ScrollArea>

      <form onSubmit={handleSubmit} className="flex items-center gap-2 border-t border-border px-4 py-3">
        <Input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Ask Nirikshak about this inspection…"
          disabled={isSending}
          className="flex-1"
        />
        <Button type="submit" size="icon" disabled={isSending || !input.trim()}>
          <Send className="size-4" />
        </Button>
      </form>

      <div className={cn("flex items-center gap-1.5 border-t border-border px-4 py-2 text-[10px] text-faint-foreground")}>
        <ShieldAlert className="size-3" />
        Compliance decisions are made only by the Rule Engine.
      </div>
    </SheetContent>
  );
}

function SuggestedAction({
  icon: Icon,
  label,
  onClick,
  disabled,
  hint,
}: {
  icon: typeof HelpCircle;
  label: string;
  onClick: () => void;
  disabled?: boolean;
  hint?: string;
}) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      title={hint}
      className={cn(
        "flex items-center gap-1.5 rounded-md px-2 py-1 text-left text-xs text-muted-foreground transition-colors",
        disabled ? "cursor-not-allowed opacity-50" : "hover:bg-surface-sunken hover:text-foreground",
      )}
    >
      <Icon className="size-3 shrink-0" />
      {label}
    </button>
  );
}

/** A quick-action chip — rounded pill, icon + label, for the four always-visible Copilot capabilities. */
function ActionChip({
  icon: Icon,
  label,
  onClick,
  disabled,
  title,
}: {
  icon: typeof HelpCircle;
  label: string;
  onClick: () => void;
  disabled?: boolean;
  title?: string;
}) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      title={title}
      className={cn(
        "flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-medium transition-colors",
        disabled
          ? "cursor-not-allowed border-border text-faint-foreground opacity-50"
          : "border-accent/30 bg-accent-soft text-accent-soft-foreground hover:border-accent/60 hover:brightness-105",
      )}
    >
      <Icon className="size-3.5" />
      {label}
    </button>
  );
}
