package ar.edu.itba.paw.webapp.DTO.Output;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.DTO.PublicUserProfileStats;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@XmlRootElement
public class PublicUserDTO {
    private long id;
    private String name;
    private String bio;
    private String travelPreferences;

    // Public Statistics
    private long totalReviewsReceived;
    private double averageRating;
    private int totalRooms;

    public PublicUserDTO() {
        // JAX-RS
    }

    public PublicUserDTO(User user, long totalReviews, double averageRating, int totalRooms) {
        this.id = user.getId();
        this.name = user.getName();
        this.bio = user.getBio();
        this.travelPreferences = user.getTravelPreferences();
        this.totalReviewsReceived = totalReviews;
        this.averageRating = averageRating;
        this.totalRooms = totalRooms;
    }

    public PublicUserDTO(PublicUserProfileStats profileStats) {
        this(
            profileStats.getUser(),
            profileStats.getTotalReviews(),
            profileStats.getAverageRating(),
            profileStats.getTotalRooms()
        );
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getBio() { return bio; }
    public String getTravelPreferences() { return travelPreferences; }
    public long getTotalReviewsReceived() { return totalReviewsReceived; }
    public double getAverageRating() { return averageRating; }
    public int getTotalRooms() { return totalRooms; }

    public void setId(long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setBio(String bio) { this.bio = bio; }
    public void setTravelPreferences(String travelPreferences) { this.travelPreferences = travelPreferences; }
    public void setTotalReviewsReceived(long totalReviewsReceived) { this.totalReviewsReceived = totalReviewsReceived; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public void setTotalRooms(int totalRooms) { this.totalRooms = totalRooms; }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, bio, travelPreferences, totalReviewsReceived, averageRating, totalRooms);
    }
}
