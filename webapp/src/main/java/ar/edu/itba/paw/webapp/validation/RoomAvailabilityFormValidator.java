package ar.edu.itba.paw.webapp.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class RoomAvailabilityFormValidator implements ConstraintValidator<ValidDateRange, Object> {

    private String startDateField;
    private String endDateField;

    @Override
    public void initialize(ValidDateRange constraintAnnotation) {
        this.startDateField = constraintAnnotation.start();
        this.endDateField = constraintAnnotation.end();
    }

    @Override
    public boolean isValid(Object form, ConstraintValidatorContext context) {
        if (form == null) return true;

        LocalDate start;
        LocalDate end;

        try {
            Method startMethod = form.getClass().getMethod(getGetterName(startDateField));
            Method endMethod = form.getClass().getMethod(getGetterName(endDateField));

            Object startValue = startMethod.invoke(form);
            Object endValue = endMethod.invoke(form);

            if (!(startValue instanceof LocalDate) || !(endValue instanceof LocalDate)) {
                return true;
            }

            start = (LocalDate) startValue;
            end = (LocalDate) endValue;

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Error al acceder a los campos de fecha para la validación: " + e.getMessage(), e);
        }

        if (start == null || end == null) {
            return true;
        }

        LocalDate today = LocalDate.now();
        boolean isValid = true;

        // ** Validación 1: Fecha de inicio no debe ser en el pasado **
        if (start.isBefore(today)) {
            isValid = false;
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("{room.error.startDate}")
                    .addPropertyNode(startDateField)
                    .addConstraintViolation();
        }

        // ** Validación 2: Fecha de fin debe ser posterior a la de inicio **
        if (end.isBefore(start)) {
            isValid = false;
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("{room.error.endDate}")
                    .addPropertyNode(endDateField)
                    .addConstraintViolation();
        }

        // ** Validación 3: Fecha de inicio no debe ser igual a la de fin **
        if(start.isEqual(end)) {
            isValid = false;
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("{room.error.sameDate}")
                    .addPropertyNode(endDateField) // Puedes apuntar el error a endDate
                    .addConstraintViolation();
        }

        return isValid;
    }

    private String getGetterName(String fieldName) {
        return "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }
}
