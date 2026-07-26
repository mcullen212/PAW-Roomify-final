package ar.edu.itba.paw.webapp.DTO.Input;

import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.*;

public class ReviewInputDTO {

    @Positive
    private long contactId;

    @Positive
    private long reviewerId;

    @Range(min = 1, max = 5, message = "{review.error.rating.invalid}")
    @NotNull(message = "{review.error.rating.notNull}")
    private Integer rating;

    @NotBlank(message = "{review.error.comment.notBlank}")
    @Pattern(
            regexp = "^[\\p{L}\\p{N}\\s\\p{P}]*$",
            message = "{review.error.message.pattern}"
    )
    @Size(max = 500, message = "{review.error.message.size}")
    private String comment;

   public long getContactId() {
        return contactId;
    }

    public void setContactId(long contactId) {
        this.contactId = contactId;
    }

    public long getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
            this.comment = comment;
        }
}
