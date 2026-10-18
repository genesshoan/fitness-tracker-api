package dev.genesshoan.fitnesstrackerapi.common.annotation;

import java.time.ZoneId;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TimezoneValidator implements ConstraintValidator<ValidTimezone, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || ZoneId.getAvailableZoneIds().contains(value);
    }
}
