/** Mirrors `com.legalmetrology.ai.copilot.dto.CopilotAnswerResponse`. */
export interface CopilotAnswerResponse {
  answer: string;
  generatedByAi: boolean;
  provider: string | null;
  answeredAt: string;
}

export type ChatTurnKind = "ask" | "explainRule" | "explainViolations" | "summarize" | "recommend" | "notes" | "compare" | "notice" | "risk";

/** A single turn rendered in the chat panel — the backend doesn't return history as one shape, so this is the frontend's own view model built from each ask() call. `kind` drives which icon/card styling a turn renders with. */
export interface ChatTurn {
  id: string;
  kind: ChatTurnKind;
  question: string;
  answer: CopilotAnswerResponse | null;
  pending: boolean;
  error: string | null;
}
