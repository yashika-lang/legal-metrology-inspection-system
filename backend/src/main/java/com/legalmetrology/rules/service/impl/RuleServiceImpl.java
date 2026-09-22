package com.legalmetrology.rules.service.impl;

import com.legalmetrology.exception.BadRequestException;
import com.legalmetrology.exception.DuplicateResourceException;
import com.legalmetrology.exception.ResourceNotFoundException;
import com.legalmetrology.inspection.entity.Rule;
import com.legalmetrology.inspection.repository.RuleRepository;
import com.legalmetrology.rules.cache.RuleCacheService;
import com.legalmetrology.rules.dto.RuleRequest;
import com.legalmetrology.rules.service.RuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleServiceImpl implements RuleService {

    private final RuleRepository ruleRepository;
    private final RuleCacheService ruleCacheService;

    @Override
    @Transactional(readOnly = true)
    public Page<Rule> list(Boolean activeOnly, Pageable pageable) {
        return Boolean.TRUE.equals(activeOnly) ? ruleRepository.findByActiveTrue(pageable) : ruleRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Rule getById(UUID id) {
        return findOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Rule> getVersionHistory(String ruleCode) {
        return ruleRepository.findByRuleCodeOrderByVersionDesc(ruleCode);
    }

    @Override
    @Transactional
    public Rule create(RuleRequest request) {
        ruleRepository.findByRuleCodeAndActiveTrue(request.ruleCode()).ifPresent(existing -> {
            throw new DuplicateResourceException(
                    "An active rule already exists for rule_code " + request.ruleCode() + " — use PUT to create a new version instead");
        });

        Rule rule = Rule.builder()
                .ruleCode(request.ruleCode())
                .title(request.title())
                .description(request.description())
                .legalReference(request.legalReference())
                .category(request.category())
                .severity(request.severity())
                .validationType(request.validationType())
                .validationExpression(request.validationExpression())
                .mandatory(request.mandatory())
                .suggestion(request.suggestion())
                .penaltyReference(request.penaltyReference())
                .version(1)
                .effectiveDate(LocalDate.now())
                .active(true)
                .build();

        rule = ruleRepository.save(rule);
        ruleCacheService.refresh();
        log.info("Rule created: {} v{}", rule.getRuleCode(), rule.getVersion());
        return rule;
    }

    @Override
    @Transactional
    public Rule update(UUID id, RuleRequest request) {
        Rule existing = findOrThrow(id);
        if (!existing.getRuleCode().equals(request.ruleCode())) {
            throw new BadRequestException("rule_code is immutable — cannot change " + existing.getRuleCode() + " to " + request.ruleCode() + " via update");
        }

        existing.setActive(false);
        // saveAndFlush, not save: Hibernate's default flush ordering runs all pending
        // INSERTs before UPDATEs regardless of call order, so a plain save() here would
        // let the new version's insert reach the database before this deactivation does —
        // tripping the partial unique index that allows only one active row per rule_code.
        ruleRepository.saveAndFlush(existing);

        Rule newVersion = Rule.builder()
                .ruleCode(existing.getRuleCode())
                .title(request.title())
                .description(request.description())
                .legalReference(request.legalReference())
                .category(request.category())
                .severity(request.severity())
                .validationType(request.validationType())
                .validationExpression(request.validationExpression())
                .mandatory(request.mandatory())
                .suggestion(request.suggestion())
                .penaltyReference(request.penaltyReference())
                .version(existing.getVersion() + 1)
                .effectiveDate(LocalDate.now())
                .active(true)
                .build();

        newVersion = ruleRepository.save(newVersion);
        ruleCacheService.refresh();
        log.info("Rule updated: {} v{} -> v{}", existing.getRuleCode(), existing.getVersion(), newVersion.getVersion());
        return newVersion;
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Rule rule = findOrThrow(id);
        rule.setActive(false);
        ruleRepository.save(rule);
        ruleCacheService.refresh();
        log.info("Rule deactivated: {} v{}", rule.getRuleCode(), rule.getVersion());
    }

    @Override
    public void refreshCache() {
        ruleCacheService.refresh();
    }

    private Rule findOrThrow(UUID id) {
        return ruleRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Rule", id));
    }
}
