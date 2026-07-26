package ar.edu.itba.paw.webapp.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class CountryUtils {

    private CountryUtils() {
        // Utility class
    }

    public static List<String> getCountries() {
        String[] countryCodes = Locale.getISOCountries();
        return Arrays.stream(countryCodes)
                .map(code -> new Locale("", code).getDisplayCountry())
                .sorted()
                .collect(Collectors.toList());
    }

    public static String getCountriesJson() {
        return getCountries().stream()
                .map(c -> "\"" + c.replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    public static boolean isValidCountry(String value) {
        return getCountryCode(value).isPresent();
    }

    public static Optional<String> getCountryCode(String value) {
        if (value == null || value.isBlank()) return Optional.empty();

        String trimmedValue = value.trim();
        String[] countryCodes = Locale.getISOCountries();

        return Arrays.stream(countryCodes)
                .map(code -> new Locale("", code))
                .flatMap(locale -> Arrays.stream(Locale.getAvailableLocales())
                        .filter(displayLocale -> locale.getDisplayCountry(displayLocale).equalsIgnoreCase(trimmedValue))
                        .map(displayLocale -> locale.getCountry()))
                .findFirst();
    }
}
