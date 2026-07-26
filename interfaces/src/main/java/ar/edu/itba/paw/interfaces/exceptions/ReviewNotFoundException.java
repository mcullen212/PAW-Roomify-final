package ar.edu.itba.paw.interfaces.exceptions;

public class ReviewNotFoundException extends RuntimeException {
    public ReviewNotFoundException(long reviewId) {
        super("Review not found with id " + reviewId);
    }

    @Override
    public String getLocalizedMessage() {
        return "error.review.notFound";
    }
}
