package ar.edu.itba.paw.webapp.DTO.Output;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@XmlRootElement
public class ReviewsDTO {

    private List<ReviewDTO> reviews;
    private int totalReviews;
    private double averageRating;

    public ReviewsDTO() {
        // Required by Jersey/MOXy.
    }

    public ReviewsDTO(final List<ReviewDTO> reviews, final int totalReviews, final double averageRating) {
        this.reviews = reviews;
        this.totalReviews = totalReviews;
        this.averageRating = averageRating;
    }

    public List<ReviewDTO> getReviews() {
        return reviews;
    }

    public void setReviews(final List<ReviewDTO> reviews) {
        this.reviews = reviews;
    }

    public int getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(final int totalReviews) {
        this.totalReviews = totalReviews;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(final double averageRating) {
        this.averageRating = averageRating;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                reviews == null ? null : reviews.stream()
                        .map(ReviewDTO::hashCode)
                        .collect(Collectors.toList()),
                totalReviews,
                averageRating
        );
    }
}
