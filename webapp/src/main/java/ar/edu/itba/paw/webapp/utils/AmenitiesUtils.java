package ar.edu.itba.paw.webapp.utils;

import ar.edu.itba.paw.model.rooms.Amenity;

import java.util.*;
import java.util.stream.Collectors;

public class AmenitiesUtils {
    private AmenitiesUtils() {
        // Utility class
    }

    public static List<String> getAmentitiesString(List<String> amenitiesFromRoom) {
        return amenitiesFromRoom.stream()
                .map(name -> Arrays.stream(Amenity.values())
                        .filter(a -> a.toString().equalsIgnoreCase(name))
                        .map(Amenity::name)
                        .findFirst()
                        .orElse(null)
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static List<Map<String,String>> getAmenities() {
        return Arrays.stream(Amenity.values())
                .map(a -> Map.of(
                        "name", a.name(),
                        "displayName", a.toString()
                ))
                .collect(Collectors.toList());
    }
}
