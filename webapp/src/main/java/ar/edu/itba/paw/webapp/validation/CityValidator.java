package ar.edu.itba.paw.webapp.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CityValidator implements ConstraintValidator<ValidCity, String> {

    private static final String CITY_REGEX = "^[\\p{L} .'-]+$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true; // skip blank
        return value.matches(CITY_REGEX);
    }
}

