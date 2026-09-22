package com.legalmetrology.rules.engine;

import com.legalmetrology.common.enums.ValidationType;
import com.legalmetrology.exception.BadRequestException;
import com.legalmetrology.rules.engine.validator.FieldValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves the {@link FieldValidator} for a rule's {@link ValidationType} (Factory over the validator Strategy interface). */
@Component
@RequiredArgsConstructor
public class ValidatorFactory {

    private final List<FieldValidator> validators;

    private Map<ValidationType, FieldValidator> validatorsByType;

    public FieldValidator resolve(ValidationType type) {
        FieldValidator validator = byType().get(type);
        if (validator == null) {
            throw new BadRequestException("No FieldValidator registered for validation type " + type);
        }
        return validator;
    }

    private Map<ValidationType, FieldValidator> byType() {
        if (validatorsByType == null) {
            validatorsByType = validators.stream()
                    .collect(Collectors.toMap(FieldValidator::supportedType, Function.identity()));
        }
        return validatorsByType;
    }
}
