package ar.edu.itba.paw.interfaces.persistence;

import ar.edu.itba.paw.model.swaps.Contact;
import ar.edu.itba.paw.model.rooms.Review;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.DTO.RoomReviewStats;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ReviewDao {
    Review save(Contact contact, User reviewer, double rating, String comment, LocalDateTime createdAt);
    boolean existsByContactAndReviewer(Long ContactId, Long reviewerId);
    double roomRating(long roomId);
    Map<Long, RoomReviewStats> getReviewStatsByRoomIds(List<Long> roomIds);
    List<Review> findByReviewerId(long userId, int page, int pageSize);
    List<Review> findByRoomId(long id, int page, int pageSize);
    int  getReviewsCountByRoomId(long roomId);
    boolean deleteReview(long reviewId);
    Optional<Review> findReviewById(long id);
    List<Review> getReviewsByRoomOfOwner(long ownerId, int page, int pageSize);
    int countReviewsByRoomOwner(long ownerId);
    double getAverageRatingByRoomOwner(long ownerId);
    int countByReviewerId(long userId);
}
