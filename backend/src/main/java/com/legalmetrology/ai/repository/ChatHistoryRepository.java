package com.legalmetrology.ai.repository;

import com.legalmetrology.ai.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory, UUID> {

    List<ChatHistory> findByInspectionIdOrderByCreatedAtAsc(UUID inspectionId);
}
