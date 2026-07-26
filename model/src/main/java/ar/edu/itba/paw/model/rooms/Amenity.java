package ar.edu.itba.paw.model.rooms;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum Amenity {
    WIFI("WiFi"),
    AC("Air Conditioning"),
    HEATING("Heating"),
    PARKING("Parking"),
    POOL("Pool"),
    GYM("Gym");

    private final String displayName;

    Amenity(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Amenity fromDisplayName(String displayName) {
        return fromString(displayName).orElse(null);
    }

    public static Amenity parse(String value) {
        return fromString(value)
                .orElseThrow(() -> new IllegalArgumentException("Unknown Amenity: " + value));
    }

    public static Optional<Amenity> fromString(String value) {
        if (value == null) {
            return Optional.empty();
        }

        String normalized = normalize(value);
        return Arrays.stream(Amenity.values())
                .filter(a -> normalize(a.name()).equals(normalized)
                        || normalize(a.displayName).equals(normalized))
                .findFirst();
    }

    private static String normalize(String value) {
        return value.replace("\"", "")
                .trim()
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return this.displayName;
    }
}
