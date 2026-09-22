package com.legalmetrology.ai.copilot.chat;

import com.legalmetrology.ai.entity.ChatHistory;

import java.util.List;
import java.util.UUID;

/**
 * Part 2 — persists and retrieves per-inspection chat history so
 * conversations are context-aware across turns. Wraps the existing
 * {@code ai.entity.ChatHistory} model (Phase 1) rather than introducing a
 * parallel one.
 */
public interface ConversationMemoryService {

    ChatHistory appendUserMessage(UUID inspectionId, UUID userId, String message);

    ChatHistory appendAssistantMessage(UUID inspectionId, UUID userId, String message, String provider, UUID reportId);

    List<ChatHistory> getHistory(UUID inspectionId);

    /** Renders recent history as plain text for injection into a prompt — bounded so long conversations don't blow the context window. */
    String getHistoryAsText(UUID inspectionId, int maxTurns);
}
