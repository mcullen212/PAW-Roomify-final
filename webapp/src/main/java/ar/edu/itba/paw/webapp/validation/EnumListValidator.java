package ar.edu.itba.paw.webapp.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumListValidator implements ConstraintValidator<ValidEnumList, List<String>> {

    private Set<String> acceptedValues;

    @Override
    public void initialize(ValidEnumList annotation) {
        acceptedValues = Arrays.stream(annotation.enumClass().getEnumConstants())
                .flatMap(value -> java.util.stream.Stream.of(value.name(), value.toString()))
                .map(EnumListValidator::normalize)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(List<String> values, ConstraintValidatorContext context) {
        if (values == null) return true;
        return values.stream()
                .allMatch(value -> value != null && acceptedValues.contains(normalize(value)));
    }

    private static String normalize(String value) {
        return value.replace("\"", "")
                .trim()
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);
    }
}
