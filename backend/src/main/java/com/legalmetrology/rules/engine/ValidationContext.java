package com.legalmetrology.rules.engine;

import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.rules.model.DeclarationSnapshot;

import java.util.Map;

/** Everything a {@link com.legalmetrology.rules.engine.validator.FieldValidator} is allowed to see for one inspection. */
public record ValidationContext(Map<DeclarationType, DeclarationSnapshot> declarations) {

    public DeclarationSnapshot get(DeclarationType type) {
        return declarations.getOrDefault(type, DeclarationSnapshot.absent(type));
    }
}
