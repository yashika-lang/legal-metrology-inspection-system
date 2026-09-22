package com.legalmetrology.ai.copilot.controller;

import com.legalmetrology.ai.copilot.chat.ConversationMemoryService;
import com.legalmetrology.ai.copilot.dto.ChatMessageResponse;
import com.legalmetrology.ai.copilot.dto.CopilotAnswerResponse;
import com.legalmetrology.ai.copilot.dto.CopilotAskRequest;
import com.legalmetrology.ai.copilot.dto.OfficerNotesRequest;
import com.legalmetrology.ai.copilot.service.CopilotService;
import com.legalmetrology.common.response.ApiResponse;
import com.legalmetrology.security.SecurityUtils;
import com.legalmetrology.utils.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Phase 3, Part 1-2: the AI Compliance Copilot. Every endpoint here only
 * explains, summarizes, recommends, or drafts language — none of them can
 * change a compliance score or a violation. See {@code CopilotService}'s
 * Javadoc.
 */
@Tag(name = "AI Copilot", description = "Explains, summarizes, and recommends — never decides compliance. All decisions come from the Rule Engine.")
@RestController
@RequestMapping("/api/v1/copilot")
@RequiredArgsConstructor
public class CopilotController {

    private final CopilotService copilotService;
    private final ConversationMemoryService conversationMemoryService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "Ask the Copilot a natural-language question about an inspection")
    @PostMapping("/inspections/{inspectionId}/ask")
    public ResponseEntity<ApiResponse<CopilotAnswerResponse>> ask(
            @PathVariable UUID inspectionId, @Valid @RequestBody CopilotAskRequest request) {
        var answer = copilotService.ask(inspectionId, request.question(), securityUtils.getCurrentUserId());
        return ResponseUtil.ok(answer);
    }

    @Operation(summary = "Get the chat history for an inspection")
    @GetMapping("/inspections/{inspectionId}/history")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> history(@PathVariable UUID inspectionId) {
        var history = conversationMemoryService.getHistory(inspectionId).stream()
                .map(h -> new ChatMessageResponse(h.getId(), h.getRole(), h.getMessage(), h.getProvider(),
                        h.getReport() != null ? h.getReport().getId() : null, h.getCreatedAt()))
                .toList();
        return ResponseUtil.ok(history);
    }

    @Operation(summary = "Explain a Legal Metrology rule in plain language")
    @GetMapping("/rules/{ruleCode}/explain")
    public ResponseEntity<ApiResponse<CopilotAnswerResponse>> explainRule(@PathVariable String ruleCode) {
        var answer = copilotService.explainRule(ruleCode, securityUtils.getCurrentUserId());
        return ResponseUtil.ok(answer);
    }

    @Operation(summary = "Generate a professional summary of an inspection")
    @PostMapping("/inspections/{inspectionId}/summarize")
    public ResponseEntity<ApiResponse<CopilotAnswerResponse>> summarize(@PathVariable UUID inspectionId) {
        var answer = copilotService.summarizeInspection(inspectionId, securityUtils.getCurrentUserId());
        return ResponseUtil.ok(answer);
    }

    @Operation(summary = "Rewrite an officer's rough field notes into professional report language")
    @PostMapping("/inspections/{inspectionId}/officer-notes")
    public ResponseEntity<ApiResponse<CopilotAnswerResponse>> officerNotes(
            @PathVariable UUID inspectionId, @Valid @RequestBody OfficerNotesRequest request) {
        var answer = copilotService.generateOfficerNotes(inspectionId, request.roughNotes(), securityUtils.getCurrentUserId());
        return ResponseUtil.ok(answer);
    }

    @Operation(summary = "Generate manufacturer-facing fix recommendations for an inspection's violations")
    @PostMapping("/inspections/{inspectionId}/manufacturer-recommendations")
    public ResponseEntity<ApiResponse<CopilotAnswerResponse>> manufacturerRecommendations(@PathVariable UUID inspectionId) {
        var answer = copilotService.generateManufacturerRecommendations(inspectionId, securityUtils.getCurrentUserId());
        return ResponseUtil.ok(answer);
    }

    @Operation(summary = "Compare two inspections (e.g. this vs. the previous one for the same product)")
    @PostMapping("/inspections/compare")
    public ResponseEntity<ApiResponse<CopilotAnswerResponse>> compare(
            @RequestParam UUID a, @RequestParam UUID b) {
        var answer = copilotService.compareInspections(a, b, securityUtils.getCurrentUserId());
        return ResponseUtil.ok(answer);
    }
}
