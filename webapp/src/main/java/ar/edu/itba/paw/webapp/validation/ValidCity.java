package ar.edu.itba.paw.webapp.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CityValidator.class)
@Documented
public @interface ValidCity {
    String message() default "{room.invalid.city}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}