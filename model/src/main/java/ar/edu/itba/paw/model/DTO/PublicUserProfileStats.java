package ar.edu.itba.paw.model.DTO;

import ar.edu.itba.paw.model.User;

public class PublicUserProfileStats {

    private final User user;
    private final long totalReviews;
    private final double averageRating;
    private final int totalRooms;

    public PublicUserProfileStats(
        User user,
        long totalReviews,
        double averageRating,
        int totalRooms
    ) {
        this.user = user;
        this.totalReviews = totalReviews;
        this.averageRating = averageRating;
        this.totalRooms = totalRooms;
    }

    public User getUser() {
        return user;
    }

    public long getTotalReviews() {
        return totalReviews;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public int getTotalRooms() {
        return totalRooms;
    }
}
