package com.legalmetrology.ai.repository;

import com.legalmetrology.ai.entity.AIResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AIResponseRepository extends JpaRepository<AIResponse, UUID> {
}
