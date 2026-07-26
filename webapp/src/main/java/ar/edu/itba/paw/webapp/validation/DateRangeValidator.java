package ar.edu.itba.paw.webapp.validation;

import ar.edu.itba.paw.model.DateRange;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.LocalDate;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, DateRange> {

    @Override
    public void initialize(ValidDateRange constraintAnnotation) {}

    @Override
    public boolean isValid(DateRange value, ConstraintValidatorContext context) {
        if (value == null) return true;

        LocalDate start = value.getStartDate();
        LocalDate end = value.getEndDate();

        if (start == null || end == null) {
            return false; // let @NotNull handle null cases
        }

        LocalDate today = LocalDate.now();

        if (start.isBefore(today)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("{room.error.startDate}")
                    .addPropertyNode("startDate")
                    .addConstraintViolation();
            return false;
        }

        if (end.isBefore(start)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("{room.error.endDate}")
                    .addPropertyNode("endDate")
                    .addConstraintViolation();
            return false;
        }

        if(start.isEqual(end)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("{room.error.sameDate}")
                    .addPropertyNode("endDate")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}





