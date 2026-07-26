package ar.edu.itba.paw.webapp.DTO.Output;

import ar.edu.itba.paw.model.rooms.Review;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.swaps.Contact;

import javax.ws.rs.core.UriInfo;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReviewDTO {

    private long id;
    private String comment;
    private double rating;
    private LocalDate date;
    private long reviewerId;
    private String reviewerName;
    private String reviewerUrl;
    private long roomId;
    private String roomTitle;
    private String roomUrl;
    private String url;

    public ReviewDTO() {
        // For Jersey
    }

    public ReviewDTO(Review review, UriInfo uriInfo) {
        this.id = review.getId();
        this.comment = review.getComment();
        this.rating = review.getRating();
        this.date = review.getCreatedAt().toLocalDate();
        this.reviewerId = review.getReviewer().getId();
        this.reviewerName = review.getReviewer().getName();

        this.reviewerUrl = uriInfo.getBaseUriBuilder()
                .path("users")
                .path(String.valueOf(this.reviewerId))
                .build().toString();

        final Room reviewedRoom = getReviewedRoom(review);
        this.roomId = reviewedRoom.getId();
        this.roomTitle = reviewedRoom.getTitle();

        this.roomUrl = uriInfo.getBaseUriBuilder()
                .path("rooms")
                .path(String.valueOf(this.roomId))
                .build().toString();
        
        this.url = uriInfo.getBaseUriBuilder()
                .path("reviews")
                .path(String.valueOf(id))
                .build().toString();
    }

    private Room getReviewedRoom(final Review review) {
        final Contact contact = review.getContact();
        if (
                contact.isSwap()
                && contact.getRoomRequested().getOwner().getId() == review.getReviewer().getId()
                && contact.getRoomOffered() != null
        ) {
            return contact.getRoomOffered();
        }

        return contact.getRoomRequested();
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public long getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public String getReviewerUrl() {
        return reviewerUrl;
    }

    public void setReviewerUrl(String reviewerUrl) {
        this.reviewerUrl = reviewerUrl;
    }

    public String getRoomUrl() {
        return roomUrl;
    }

    public void setRoomUrl(String roomUrl) {
        this.roomUrl = roomUrl;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public String getRoomTitle() {
        return roomTitle;
    }

    public void setRoomTitle(String roomTitle) {
        this.roomTitle = roomTitle;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, comment, rating, date, reviewerId, reviewerName, reviewerUrl, roomId, roomTitle, roomUrl, url);
    }

    public static int collectionHashCode(final Long roomId,
                                         final Long userId,
                                         final int page,
                                         final int pageSize,
                                         final int totalReviews,
                                         final int totalPages,
                                         final List<ReviewDTO> reviews) {
        return Objects.hash(
                roomId,
                userId,
                page,
                pageSize,
                totalReviews,
                totalPages,
                reviews.stream()
                        .map(ReviewDTO::hashCode)
                        .collect(Collectors.toList())
        );
    }
}
