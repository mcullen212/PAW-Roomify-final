package ar.edu.itba.paw.interfaces.service;

import ar.edu.itba.paw.interfaces.exceptions.BusinessException;
import ar.edu.itba.paw.model.DTO.ReviewPageDTO;
import ar.edu.itba.paw.model.rooms.Review;

import java.util.List;
import java.util.Locale;

public interface ReviewService {
    Review addReview(long contactId, long reviewerId, double rating, String comment, Locale locale)
            throws BusinessException;
    boolean isReviewPending(long contactId, String email);
    ReviewPageDTO getReviewsPage(Long roomId, Long userId, Long roomOwnerId, int page, int pageSize);
    List<Review> getReviews(long roomId, int page, int pageSize);
    List<Review> getReviewsByUserId(long userId, int page, int pageSize);
    Review findReviewById(long id);
    double getRoomRating(long roomId);
    void deleteReview(long reviewId);
    List<Review> getReviewsByRoomOfOwner(long id, int page, int pageSize);
    int countReviewsByRoomOwner(long ownerId);
    double getAverageRatingByRoomOwner(long ownerId);
    int countReviewsByRoomOwnerId(long id);
    double getAverageRatingByRoomOwnerId(long id);
    boolean isOwner(String email, long id);
    int countReviewsByUserId(long userId);
    int countReviewsByRoomId(long roomId);
}
