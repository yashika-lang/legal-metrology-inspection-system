package com.legalmetrology.inspection.repository;

import com.legalmetrology.inspection.entity.Rule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RuleRepository extends JpaRepository<Rule, UUID> {

    /** The single currently-active version of a rule, if any (enforced by a partial unique index on rule_code where is_active). */
    Optional<Rule> findByRuleCodeAndActiveTrue(String ruleCode);

    /** Every version ever created for a rule code, newest first — the version history. */
    List<Rule> findByRuleCodeOrderByVersionDesc(String ruleCode);

    /** What the Rule Loader caches on startup/refresh. */
    List<Rule> findByActiveTrue();

    Page<Rule> findByActiveTrue(Pageable pageable);
}
