package com.legalmetrology.validation;

import com.legalmetrology.utils.ValidationUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BarcodeConstraintValidator implements ConstraintValidator<ValidBarcode, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // pair with @NotBlank when the field is mandatory
        }
        return ValidationUtil.isValidBarcode(value);
    }
}
