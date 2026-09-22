import { useState } from "react";
import { copilotApi } from "../api/copilotApi";
import type { ChatTurn, ChatTurnKind, CopilotAnswerResponse } from "../types";

export function useCopilotChat(inspectionId: string) {
  const [turns, setTurns] = useState<ChatTurn[]>([]);
  const [isSending, setIsSending] = useState(false);

  /** Every suggested action funnels through here — only the label shown, the kind (for card styling), and the promise that produces the answer differ per action. */
  async function runAction(kind: ChatTurnKind, label: string, resolveAnswer: () => Promise<CopilotAnswerResponse>) {
    const id = crypto.randomUUID();
    setTurns((prev) => [...prev, { id, kind, question: label, answer: null, pending: true, error: null }]);
    setIsSending(true);
    try {
      const answer = await resolveAnswer();
      setTurns((prev) => prev.map((t) => (t.id === id ? { ...t, answer, pending: false } : t)));
    } catch {
      setTurns((prev) =>
        prev.map((t) => (t.id === id ? { ...t, pending: false, error: "The Copilot couldn't answer that just now." } : t)),
      );
    } finally {
      setIsSending(false);
    }
  }

  function ask(question: string) {
    return runAction("ask", question, () => copilotApi.ask(inspectionId, question));
  }

  function explainRule(ruleCode: string) {
    return runAction("explainRule", `Explain rule ${ruleCode}`, () => copilotApi.explainRule(ruleCode));
  }

  function summarize() {
    return runAction("summarize", "Summarize this inspection", () => copilotApi.summarize(inspectionId));
  }

  function howToFix() {
    return runAction("recommend", "How do I fix this?", () => copilotApi.manufacturerRecommendations(inspectionId));
  }

  /**
   * These three have no dedicated backend endpoint — there is no
   * `/explain-violations`, `/generate-notice`, or `/risk-analysis` route.
   * Each is a real call to the same `ask()` endpoint the free-text input
   * uses, with a specific, well-formed question — the LLM call, the
   * `CopilotContext` it reasons over, and the answer are all completely
   * real; only the *prompt* is canned rather than officer-typed. This
   * matches how "Why Failed?" already works.
   */
  function explainViolations() {
    return runAction("explainViolations", "Explain all violations", () =>
      copilotApi.ask(inspectionId, "Explain each violation found in this inspection in detail — what's wrong, and why it matters legally under the cited rule."),
    );
  }

  function generateNotice() {
    return runAction("notice", "Generate compliance notice", () =>
      copilotApi.ask(
        inspectionId,
        "Draft a formal Legal Metrology compliance notice addressed to the manufacturer for this inspection, citing the specific rule violations, the legal references, and the corrective action required for each.",
      ),
    );
  }

  function analyzeRisk() {
    return runAction("risk", "Risk analysis", () =>
      copilotApi.ask(
        inspectionId,
        "Analyze the fraud risk for this inspection — explain what's driving the current risk rating based on the compliance score and the severity/pattern of violations found.",
      ),
    );
  }

  function generateOfficerNotes(roughNotes: string) {
    return runAction("notes", "Generate officer notes", () => copilotApi.generateOfficerNotes(inspectionId, roughNotes));
  }

  function compareWith(otherInspectionId: string) {
    return runAction("compare", `Compare with inspection ${otherInspectionId.slice(0, 8)}…`, () =>
      copilotApi.compareInspections(inspectionId, otherInspectionId),
    );
  }

  return {
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
  };
}
