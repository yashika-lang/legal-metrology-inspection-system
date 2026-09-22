package com.legalmetrology.rules.engine.custom;

import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.inspection.entity.Rule;
import com.legalmetrology.rules.engine.ValidationContext;
import com.legalmetrology.rules.model.DeclarationSnapshot;
import com.legalmetrology.rules.model.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Registered as {@code "IMPORTER_REQUIRES_ORIGIN"}: country of origin is
 * only legally mandatory when the package declares an importer (i.e. it's
 * an imported good) — a conditional-on-another-field rule none of the 11
 * generic single-field validator types can express.
 */
@Component
public class ImporterRequiresOriginStrategy implements CustomRuleStrategy {

    @Override
    public String key() {
        return "IMPORTER_REQUIRES_ORIGIN";
    }

    @Override
    public ValidationResult evaluate(Rule rule, ValidationContext context) {
        DeclarationSnapshot importer = context.get(DeclarationType.IMPORTER);
        DeclarationSnapshot origin = context.get(DeclarationType.COUNTRY_OF_ORIGIN);

        if (!importer.present()) {
            return ValidationResult.pass(rule, DeclarationType.COUNTRY_OF_ORIGIN, origin.value(), importer.confidence(), origin.boundingBox(),
                    "No importer was declared, so country of origin is not required by this rule.");
        }
        if (origin.present()) {
            return ValidationResult.pass(rule, DeclarationType.COUNTRY_OF_ORIGIN, origin.value(), origin.confidence(), origin.boundingBox(),
                    "An importer was declared, and country of origin (\"" + origin.value() + "\") is also present, as required.");
        }
        return ValidationResult.fail(rule, DeclarationType.COUNTRY_OF_ORIGIN, null, "present", origin.confidence(), origin.boundingBox(),
                "An importer was declared (\"" + importer.value() + "\") but no country of origin was found — imported goods must declare their origin.");
    }
}
