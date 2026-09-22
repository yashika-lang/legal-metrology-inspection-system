package com.legalmetrology.rules.service;

import com.legalmetrology.inspection.entity.Rule;
import com.legalmetrology.rules.dto.RuleRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface RuleService {

    Page<Rule> list(Boolean activeOnly, Pageable pageable);

    Rule getById(UUID id);

    List<Rule> getVersionHistory(String ruleCode);

    Rule create(RuleRequest request);

    /** Never mutates the existing row — creates a new version and deactivates the previous one. See {@code Rule}'s Javadoc. */
    Rule update(UUID id, RuleRequest request);

    /** Soft delete (active = false) — a hard delete would break the FK from historical Violations. */
    void delete(UUID id);

    void refreshCache();
}
