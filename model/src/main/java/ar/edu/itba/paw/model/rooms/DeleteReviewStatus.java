package ar.edu.itba.paw.model.rooms;

public enum DeleteReviewStatus {
    SUCCESS,
    REVIEW_NOT_FOUND,
    NOT_OWNER, // El usuario no es el autor de la review
    PERSISTENCE_ERROR
}