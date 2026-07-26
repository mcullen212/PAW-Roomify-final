package ar.edu.itba.paw.webapp.validation;

import ar.edu.itba.paw.webapp.utils.CountryUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CountryValidator implements ConstraintValidator<ValidCountry, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return CountryUtils.isValidCountry(value);
    }
}

