package ar.edu.itba.paw.model.DTO;

import ar.edu.itba.paw.model.User;

import java.math.BigDecimal;

public class UserProfileStats {

    private final User user;
    private final long totalReviews;
    private final long totalWrittenReviews;
    private final double reviewAvg;
    private final BigDecimal totalEarned;
    private final BigDecimal totalSpent;
    private final long totalSwaps;

    public UserProfileStats(
        User user,
        long totalReviews,
        long totalWrittenReviews,
        double reviewAvg,
        BigDecimal totalEarned,
        BigDecimal totalSpent,
        long totalSwaps
    ) {
        this.user = user;
        this.totalReviews = totalReviews;
        this.totalWrittenReviews = totalWrittenReviews;
        this.reviewAvg = reviewAvg;
        this.totalEarned = totalEarned;
        this.totalSpent = totalSpent;
        this.totalSwaps = totalSwaps;
    }

    public User getUser() {
        return user;
    }

    public long getTotalReviews() {
        return totalReviews;
    }

    public long getTotalWrittenReviews() {
        return totalWrittenReviews;
    }

    public double getReviewAvg() {
        return reviewAvg;
    }

    public BigDecimal getTotalEarned() {
        return totalEarned;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public long getTotalSwaps() {
        return totalSwaps;
    }
}
