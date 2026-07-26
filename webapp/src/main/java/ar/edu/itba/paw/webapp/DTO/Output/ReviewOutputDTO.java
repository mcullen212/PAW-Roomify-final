package ar.edu.itba.paw.webapp.DTO.Output;

import ar.edu.itba.paw.model.rooms.Review;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ReviewOutputDTO {
    private long id;
    private String comment;
    private double rating;
    private String userEmail;

    public ReviewOutputDTO() {}

    public ReviewOutputDTO(Review review) {
        this.id = review.getId();
        this.comment = review.getComment();
        this.rating = review.getRating();
        this.userEmail = review.getReviewer().getEmail();
    }

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
    public String getUserEmail() {
        return userEmail;
    }
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
}
