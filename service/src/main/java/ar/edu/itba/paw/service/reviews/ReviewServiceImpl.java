package ar.edu.itba.paw.service.reviews;

import ar.edu.itba.paw.interfaces.persistence.ReviewDao;
import ar.edu.itba.paw.interfaces.service.ContactService;
import ar.edu.itba.paw.interfaces.service.ReviewService;
import ar.edu.itba.paw.interfaces.service.UserService;
import ar.edu.itba.paw.interfaces.exceptions.BusinessException;
import ar.edu.itba.paw.interfaces.exceptions.ContactNotFoundException;
import ar.edu.itba.paw.interfaces.exceptions.ReviewNotFoundException;
import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.DTO.ReviewPageDTO;
import ar.edu.itba.paw.model.rooms.Review;
import ar.edu.itba.paw.model.swaps.Contact;
import org.springframework.context.MessageSource;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ReviewServiceImpl implements ReviewService {
    private final ReviewDao reviewDao;
    private final ContactService contactService;
    private final UserService userService;
    private final MessageSource messageSource;
    private final ReviewEligibilityService reviewEligibilityService;

    public ReviewServiceImpl(ReviewDao reviewDao, ContactService contactService, UserService userService,
                             MessageSource messageSource, ReviewEligibilityService reviewEligibilityService) {
        this.reviewDao = reviewDao;
        this.contactService = contactService;
        this.userService = userService;
        this.messageSource = messageSource;
        this.reviewEligibilityService = reviewEligibilityService;
    }

    @Override
    @Transactional
    public Review addReview(long contactId, long reviewerId, double rating, String comment, Locale locale)
            throws BusinessException {
        User user = userService.findUserById(reviewerId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Contact contact = contactService.getContactById(contactId)
                .orElseThrow(() -> new ContactNotFoundException(contactId));


        assertCanLeaveReview(contact, user, locale);

        if (reviewDao.existsByContactAndReviewer(contactId, user.getId())) {
            throw new BusinessException(messageSource.getMessage("review.error.already.exists", null, locale));
        }

        return reviewDao.save(contact, user, rating, comment, LocalDateTime.now());
    }

    private void assertCanLeaveReview(Contact contact, User user, Locale locale) throws BusinessException {
        ReviewEligibilityService.EligibilityStatus eligibilityStatus =
                reviewEligibilityService.getLeaveReviewEligibility(contact, user, LocalDate.now());

        switch (eligibilityStatus) {
            case INVALID_STATUS:
                throw new BusinessException(messageSource.getMessage("review.error.cannot.review.status", null, locale));
            case TRIP_NOT_FINISHED:
                throw new BusinessException(messageSource.getMessage("review.error.cannot.review.date", null, locale));
            case NON_SWAP_OWNER:
                throw new BusinessException(messageSource.getMessage("review.status.owner", null, locale));
            case ELIGIBLE:
            default:
                return;
        }
    }

    @Override
    public boolean isReviewPending(long contactId, String email) {
        User user = userService.findUserByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Optional<Contact> contactOpt = contactService.getContactById(contactId);
        if (contactOpt.isEmpty()) {
            return false;
        }
        Contact contact = contactOpt.get();

        return reviewEligibilityService.isReviewPending(contact, user, LocalDate.now());
    }

    @Override
    public ReviewPageDTO getReviewsPage(Long roomId, Long userId, Long roomOwnerId, int page, int pageSize) {
        final int safePageSize = Math.max(pageSize, 1);
        final int requestedPage = Math.max(page, 1);

        if (roomId != null) {
            final int totalReviews = reviewDao.getReviewsCountByRoomId(roomId);
            final int safePage = getSafePage(requestedPage, totalReviews, safePageSize);
            final double averageRating = totalReviews == 0 ? 0 : getRoomRating(roomId);
            return new ReviewPageDTO(
                    reviewDao.findByRoomId(roomId, safePage, safePageSize),
                    totalReviews,
                    averageRating,
                    safePageSize
            );
        }

        if (userId != null) {
            final int totalReviews = reviewDao.countByReviewerId(userId);
            final int safePage = getSafePage(requestedPage, totalReviews, safePageSize);
            return new ReviewPageDTO(
                    reviewDao.findByReviewerId(userId, safePage, safePageSize),
                    totalReviews,
                    0,
                    safePageSize
            );
        }

        if (roomOwnerId != null) {
            final int totalReviews = reviewDao.countReviewsByRoomOwner(roomOwnerId);
            final int safePage = getSafePage(requestedPage, totalReviews, safePageSize);
            return new ReviewPageDTO(
                    reviewDao.getReviewsByRoomOfOwner(roomOwnerId, safePage, safePageSize),
                    totalReviews,
                    0,
                    safePageSize
            );
        }

        throw new IllegalArgumentException("roomId, userId or roomOwnerId is required");
    }

    private int getSafePage(final int requestedPage, final int totalItems, final int pageSize) {
        final int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        if (totalPages <= 0) {
            return 1;
        }
        return Math.min(requestedPage, totalPages);
    }

    @Override
    public List<Review> getReviews(long roomId, int page, int pageSize) {
        return reviewDao.findByRoomId(roomId, page, pageSize);
    }

    @Override
    public List<Review> getReviewsByUserId(long userId, int page, int pageSize) {
        return reviewDao.findByReviewerId(userId, page, pageSize);
    }

    @Override
    public Review findReviewById(long id) {
        return reviewDao.findReviewById(id).orElse(null);
    }

    @Override
    public int countReviewsByRoomId(long roomId){
        return reviewDao.getReviewsCountByRoomId(roomId);
    }

    @Override
    public int countReviewsByUserId(long userId) {
        return reviewDao.countByReviewerId(userId);
    }

    @Override
    public double getRoomRating(long roomId) {
        return reviewDao.roomRating(roomId);
    }

    @Override
    @Transactional
    public void deleteReview(long reviewId) {
        if (!reviewDao.deleteReview(reviewId)) {
            throw new ReviewNotFoundException(reviewId);
        }
    }

    @Transactional
    @Override
    public List<Review> getReviewsByRoomOfOwner(long id, int page, int pageSize){
        return reviewDao.getReviewsByRoomOfOwner(id, page, pageSize);
    }

    @Override
    public int countReviewsByRoomOwner(long ownerId){
        return reviewDao.countReviewsByRoomOwner(ownerId);
    }

    @Override
    public double getAverageRatingByRoomOwner(long ownerId){
        return reviewDao.getAverageRatingByRoomOwner(ownerId);
    }

    @Override
    public int countReviewsByRoomOwnerId(long id){
        User user = userService.findUserById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return countReviewsByRoomOwner(user.getId());
    }

    @Override
    public double getAverageRatingByRoomOwnerId(long id) {
        User user = userService.findUserById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return getAverageRatingByRoomOwner(user.getId());
    }

    @Override
    public boolean isOwner(String email, long id) {
        User user = userService.findUserByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Review review = reviewDao.findReviewById(id)
                .orElseThrow(() -> new ReviewNotFoundException(id));
        return user.getId() == review.getReviewer().getId();
    }
}
