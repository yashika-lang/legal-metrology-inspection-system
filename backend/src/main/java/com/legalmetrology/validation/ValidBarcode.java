package com.legalmetrology.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Accepts EAN-8, UPC-A, EAN-13, or ITF-14 numeric barcode formats (blank allowed unless combined with @NotBlank). */
@Documented
@Constraint(validatedBy = BarcodeConstraintValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBarcode {

    String message() default "Barcode must be a valid EAN-8, UPC-A, EAN-13, or ITF-14 numeric code";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
