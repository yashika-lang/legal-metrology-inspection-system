package com.legalmetrology.rules.cache;

import com.legalmetrology.inspection.entity.Rule;
import com.legalmetrology.inspection.repository.RuleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The Rule Loader: reads every active {@link Rule} into memory once on
 * startup so evaluating an inspection never hits the database per-rule,
 * and exposes {@link #refresh()} so an admin editing rules through the API
 * doesn't need to restart the application for the change to take effect
 * (see {@code RuleController#refreshCache}).
 * <p>
 * Holds an immutable snapshot behind an {@link AtomicReference} — a refresh
 * swaps the whole list atomically, so an evaluation in progress always sees
 * one consistent set of rules, never a partially-updated one.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleCacheService {

    private final RuleRepository ruleRepository;

    private final AtomicReference<List<Rule>> cachedActiveRules = new AtomicReference<>(List.of());

    @PostConstruct
    public void loadOnStartup() {
        refresh();
    }

    public void refresh() {
        List<Rule> activeRules = ruleRepository.findByActiveTrue();
        cachedActiveRules.set(List.copyOf(activeRules));
        log.info("Rule cache loaded: {} active rules", activeRules.size());
    }

    public List<Rule> getActiveRules() {
        return cachedActiveRules.get();
    }
}
