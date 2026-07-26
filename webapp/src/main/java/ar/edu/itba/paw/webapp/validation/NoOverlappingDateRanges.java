package ar.edu.itba.paw.webapp.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NoOverlappingDateRangesValidator.class)
@Documented
public @interface NoOverlappingDateRanges {
    String message() default "{room.error.overlappingDates}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}