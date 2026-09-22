import { apiClient, unwrap } from "@/services/apiClient";
import type { ApiResponse } from "@/types/api.types";
import type { CopilotAnswerResponse } from "../types";

/**
 * Every one of these hits a distinct real backend endpoint — the Copilot
 * is not a single generic "ask anything" surface. Suggested questions
 * that map to a dedicated capability (explain a specific rule, draft
 * officer notes from rough input, compare two inspections) call that
 * capability's actual endpoint with its actual required parameters,
 * rather than funnelling everything through free-text `ask()`.
 */
export const copilotApi = {
  ask: (inspectionId: string, question: string) =>
    unwrap(
      apiClient.post<ApiResponse<CopilotAnswerResponse>>(`/copilot/inspections/${inspectionId}/ask`, { question }),
    ),

  explainRule: (ruleCode: string) =>
    unwrap(apiClient.get<ApiResponse<CopilotAnswerResponse>>(`/copilot/rules/${ruleCode}/explain`)),

  summarize: (inspectionId: string) =>
    unwrap(apiClient.post<ApiResponse<CopilotAnswerResponse>>(`/copilot/inspections/${inspectionId}/summarize`)),

  generateOfficerNotes: (inspectionId: string, roughNotes: string) =>
    unwrap(
      apiClient.post<ApiResponse<CopilotAnswerResponse>>(`/copilot/inspections/${inspectionId}/officer-notes`, {
        roughNotes,
      }),
    ),

  manufacturerRecommendations: (inspectionId: string) =>
    unwrap(
      apiClient.post<ApiResponse<CopilotAnswerResponse>>(
        `/copilot/inspections/${inspectionId}/manufacturer-recommendations`,
      ),
    ),

  compareInspections: (inspectionIdA: string, inspectionIdB: string) =>
    unwrap(
      apiClient.post<ApiResponse<CopilotAnswerResponse>>("/copilot/inspections/compare", null, {
        params: { a: inspectionIdA, b: inspectionIdB },
      }),
    ),
};
