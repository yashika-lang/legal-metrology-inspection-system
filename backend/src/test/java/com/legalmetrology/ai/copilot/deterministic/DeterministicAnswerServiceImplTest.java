package com.legalmetrology.ai.copilot.deterministic;

import com.legalmetrology.ai.copilot.context.CopilotContext;
import com.legalmetrology.ai.copilot.deterministic.impl.DeterministicAnswerServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicAnswerServiceImplTest {

    private final DeterministicAnswerServiceImpl service = new DeterministicAnswerServiceImpl();

    @Test
    void answersComplianceScoreQuestionDirectlyFromContext() {
        CopilotContext context = context(BigDecimal.valueOf(72.5), "LOW", List.of());

        var answer = service.tryAnswer(context, "What is the compliance score?");

        assertThat(answer).isPresent();
        assertThat(answer.get()).contains("72.5");
    }

    @Test
    void answersMostCriticalViolationByPickingTheHighestSeverity() {
        var minor = new CopilotContext.ViolationSummary("LM-CONSCARE-001", "Consumer care", "MINOR", "CUSTOMER_CARE",
                "Missing consumer care", null, "present", BigDecimal.ONE, null, null);
        var critical = new CopilotContext.ViolationSummary("LM-MRP-001", "MRP", "CRITICAL", "MRP",
                "MRP missing", null, "present", BigDecimal.ONE, null, null);
        CopilotContext context = context(BigDecimal.valueOf(40), "HIGH", List.of(minor, critical));

        var answer = service.tryAnswer(context, "Which violation is the most critical?");

        assertThat(answer).isPresent();
        assertThat(answer.get()).contains("LM-MRP-001");
        assertThat(answer.get()).contains("CRITICAL");
    }

    @Test
    void answersMissingDeclarationsFromContext() {
        var present = new CopilotContext.DeclarationSummary("NET_QUANTITY", true, "500g", BigDecimal.ONE, null, null);
        var missing = new CopilotContext.DeclarationSummary("MRP", false, null, BigDecimal.ZERO, null, null);
        CopilotContext context = new CopilotContext(UUID.randomUUID(), "Product", "Manufacturer", "Category", "COMPLETED",
                BigDecimal.valueOf(50), "MEDIUM", List.of(), List.of(present, missing), List.of());

        var answer = service.tryAnswer(context, "What declarations are missing?");

        assertThat(answer).isPresent();
        assertThat(answer.get()).contains("MRP");
        assertThat(answer.get()).doesNotContain("NET_QUANTITY");
    }

    @Test
    void fallsThroughToEmptyForQuestionsItDoesNotRecognize() {
        CopilotContext context = context(BigDecimal.valueOf(80), "LOW", List.of());

        var answer = service.tryAnswer(context, "How can the manufacturer fix this?");

        assertThat(answer).isEmpty();
    }

    private CopilotContext context(BigDecimal score, String risk, List<CopilotContext.ViolationSummary> violations) {
        return new CopilotContext(UUID.randomUUID(), "Product", "Manufacturer", "Category", "COMPLETED",
                score, risk, violations, List.of(), List.of());
    }
}
