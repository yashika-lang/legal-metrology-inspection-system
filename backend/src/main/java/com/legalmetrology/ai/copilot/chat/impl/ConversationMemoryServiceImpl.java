package com.legalmetrology.ai.copilot.chat.impl;

import com.legalmetrology.ai.copilot.chat.ConversationMemoryService;
import com.legalmetrology.ai.entity.ChatHistory;
import com.legalmetrology.ai.repository.ChatHistoryRepository;
import com.legalmetrology.auth.entity.User;
import com.legalmetrology.auth.repository.UserRepository;
import com.legalmetrology.exception.ResourceNotFoundException;
import com.legalmetrology.inspection.entity.Inspection;
import com.legalmetrology.inspection.repository.InspectionRepository;
import com.legalmetrology.report.repository.ComplianceReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationMemoryServiceImpl implements ConversationMemoryService {

    private static final String ROLE_USER = "USER";
    private static final String ROLE_ASSISTANT = "ASSISTANT";

    private final ChatHistoryRepository chatHistoryRepository;
    private final InspectionRepository inspectionRepository;
    private final UserRepository userRepository;
    private final ComplianceReportRepository complianceReportRepository;

    @Override
    @Transactional
    public ChatHistory appendUserMessage(UUID inspectionId, UUID userId, String message) {
        return save(inspectionId, userId, ROLE_USER, message, null, null);
    }

    @Override
    @Transactional
    public ChatHistory appendAssistantMessage(UUID inspectionId, UUID userId, String message, String provider, UUID reportId) {
        return save(inspectionId, userId, ROLE_ASSISTANT, message, provider, reportId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatHistory> getHistory(UUID inspectionId) {
        return chatHistoryRepository.findByInspectionIdOrderByCreatedAtAsc(inspectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public String getHistoryAsText(UUID inspectionId, int maxTurns) {
        List<ChatHistory> history = getHistory(inspectionId);
        int fromIndex = Math.max(0, history.size() - maxTurns);
        List<ChatHistory> recent = history.subList(fromIndex, history.size());

        if (recent.isEmpty()) {
            return "(no prior conversation)";
        }
        StringBuilder text = new StringBuilder();
        for (ChatHistory turn : recent) {
            text.append(turn.getRole()).append(": ").append(turn.getMessage()).append('\n');
        }
        return text.toString();
    }

    private ChatHistory save(UUID inspectionId, UUID userId, String role, String message, String provider, UUID reportId) {
        User user = userRepository.findById(userId).orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        Inspection inspection = inspectionId != null
                ? inspectionRepository.findById(inspectionId).orElseThrow(() -> ResourceNotFoundException.of("Inspection", inspectionId))
                : null;

        ChatHistory chatHistory = ChatHistory.builder()
                .user(user)
                .inspection(inspection)
                .role(role)
                .message(message)
                .provider(provider)
                .report(reportId != null ? complianceReportRepository.findById(reportId).orElse(null) : null)
                .build();

        return chatHistoryRepository.save(chatHistory);
    }
}
