package ar.edu.itba.paw.model.rooms;


public enum BedType {
    TWIN,
    QUEEN,
    KING;

    @Override
    public String toString() {
        // Devuelve "Twin", "Queen", "King" en lugar de "TWIN", "QUEEN", ...
        String lower = name().toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
