package ar.edu.itba.paw.webapp.DTO.Output;

import ar.edu.itba.paw.model.DTO.UserProfileStats;
import ar.edu.itba.paw.model.User;

import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlRootElement;
import java.math.BigDecimal;
import java.util.Objects;

@XmlRootElement
public class ProfileStatsDTO extends UserDTO {
    private long totalReviews;
    private long totalWrittenReviews;
    private double reviewAvg;

    private BigDecimal totalEarned;
    private BigDecimal totalSpent;
    private long totalSwaps;

    public ProfileStatsDTO() {
        // Required by JAX-RS
    }

    public ProfileStatsDTO(final User user, long totalReviews, long totalWrittenReviews, double reviewAvg,
                           BigDecimal totalEarned, BigDecimal totalSpent, long totalSwaps) {
        super(user);

        this.totalReviews = totalReviews;
        this.totalWrittenReviews = totalWrittenReviews;
        this.reviewAvg = reviewAvg;
        this.totalEarned = totalEarned;
        this.totalSpent = totalSpent;
        this.totalSwaps = totalSwaps;
    }

    public ProfileStatsDTO(final User user, long totalReviews, long totalWrittenReviews, double reviewAvg,
                           BigDecimal totalEarned, BigDecimal totalSpent, long totalSwaps, final UriInfo uriInfo) {
        super(user, uriInfo);

        this.totalReviews = totalReviews;
        this.totalWrittenReviews = totalWrittenReviews;
        this.reviewAvg = reviewAvg;
        this.totalEarned = totalEarned;
        this.totalSpent = totalSpent;
        this.totalSwaps = totalSwaps;
    }

    public ProfileStatsDTO(final UserProfileStats profileStats) {
        this(
            profileStats.getUser(),
            profileStats.getTotalReviews(),
            profileStats.getTotalWrittenReviews(),
            profileStats.getReviewAvg(),
            profileStats.getTotalEarned(),
            profileStats.getTotalSpent(),
            profileStats.getTotalSwaps()
        );
    }

    public ProfileStatsDTO(final UserProfileStats profileStats, final UriInfo uriInfo) {
        this(
            profileStats.getUser(),
            profileStats.getTotalReviews(),
            profileStats.getTotalWrittenReviews(),
            profileStats.getReviewAvg(),
            profileStats.getTotalEarned(),
            profileStats.getTotalSpent(),
            profileStats.getTotalSwaps(),
            uriInfo
        );
    }

    public long getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(long totalReviews) {
        this.totalReviews = totalReviews;
    }

    public long getTotalWrittenReviews() {
        return totalWrittenReviews;
    }

    public void setTotalWrittenReviews(long totalWrittenReviews) {
        this.totalWrittenReviews = totalWrittenReviews;
    }

    public double getReviewAvg() {
        return reviewAvg;
    }

    public void setReviewAvg(double reviewAvg) {
        this.reviewAvg = reviewAvg;
    }

    public BigDecimal getTotalEarned() {
        return totalEarned;
    }

    public void setTotalEarned(BigDecimal totalEarned) {
        this.totalEarned = totalEarned;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }

    public long getTotalSwaps() {
        return totalSwaps;
    }

    public void setTotalSwaps(long totalSwaps) {
        this.totalSwaps = totalSwaps;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), totalReviews, totalWrittenReviews, reviewAvg, totalEarned, totalSpent, totalSwaps);
    }
}
