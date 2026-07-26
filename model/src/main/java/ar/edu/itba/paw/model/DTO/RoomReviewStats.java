package ar.edu.itba.paw.model.DTO;

public class RoomReviewStats {
    private final long roomId;
    private final int totalReviews;
    private final double averageRating;

    public RoomReviewStats(long roomId, int totalReviews, double averageRating) {
        this.roomId = roomId;
        this.totalReviews = totalReviews;
        this.averageRating = averageRating;
    }

    public long getRoomId() {
        return roomId;
    }

    public int getTotalReviews() {
        return totalReviews;
    }

    public double getAverageRating() {
        return averageRating;
    }
}
