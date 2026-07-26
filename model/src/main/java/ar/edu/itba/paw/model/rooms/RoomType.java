package ar.edu.itba.paw.model.rooms;

import java.util.stream.Stream;

public enum RoomType {
    HOME( "fas fa-home"),
    PRIVATE("fas fa-user"),
    SHARED("fas fa-users"),
    STUDIO("fas fa-building");

    private final String iconClass;

    RoomType(String iconClass) {
        this.iconClass = iconClass;
    }

    public String getIconClass() {
        return iconClass;
    }

    public static RoomType fromString(String name) {
        return Stream.of(RoomType.values())
                .filter(c -> c.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown RoomType: " + name));
    }

    @Override
    public String toString() {
        String lower = name().toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}