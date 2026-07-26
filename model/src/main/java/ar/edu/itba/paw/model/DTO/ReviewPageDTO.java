package ar.edu.itba.paw.model.DTO;

import ar.edu.itba.paw.model.rooms.Review;

import java.util.List;

public class ReviewPageDTO {

    private final List<Review> reviews;
    private final int totalReviews;
    private final int totalPages;
    private final double averageRating;

    public ReviewPageDTO(List<Review> reviews, int totalReviews, double averageRating, int pageSize) {
        this.reviews = reviews;
        this.totalReviews = totalReviews;
        this.averageRating = averageRating;
        this.totalPages = pageSize <= 0 ? 0 : (int) Math.ceil((double) totalReviews / pageSize);
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public int getTotalReviews() {
        return totalReviews;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public double getAverageRating() {
        return averageRating;
    }
}
