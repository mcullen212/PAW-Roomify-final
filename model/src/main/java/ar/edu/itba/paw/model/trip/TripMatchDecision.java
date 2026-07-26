package ar.edu.itba.paw.model.trip;

public enum TripMatchDecision {
    /* Hay un trip activo del usuario en ese pais cuyo rango contiene las fechas de la reserva. */
    CONTAINED,
    /* El pais coincide con un trip activo del usuario, pero ninguno contiene las fechas de la reserva. */
    DATES_OUTSIDE,
    /* El usuario no tiene ningun trip activo en ese pais. */
    NONE
}
