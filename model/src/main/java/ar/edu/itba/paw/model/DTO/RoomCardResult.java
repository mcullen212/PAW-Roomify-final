package ar.edu.itba.paw.model.DTO;

import ar.edu.itba.paw.model.rooms.Room;

public class RoomCardResult {
    private final Room room;
    private final RoomReviewStats reviewStats;

    public RoomCardResult(Room room, RoomReviewStats reviewStats) {
        this.room = room;
        this.reviewStats = reviewStats;
    }

    public Room getRoom() {
        return room;
    }

    public int getTotalReviews() {
        return reviewStats == null ? 0 : reviewStats.getTotalReviews();
    }

    public double getAverageRating() {
        return reviewStats == null ? 0 : reviewStats.getAverageRating();
    }
}
