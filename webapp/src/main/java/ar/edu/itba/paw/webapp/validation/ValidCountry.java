package ar.edu.itba.paw.webapp.validation;
import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CountryValidator.class)
@Documented
public @interface ValidCountry {
    String message() default "{room.invalid.country}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

