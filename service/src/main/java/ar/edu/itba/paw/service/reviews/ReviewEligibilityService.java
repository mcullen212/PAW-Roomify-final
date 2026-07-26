package ar.edu.itba.paw.service.reviews;

import ar.edu.itba.paw.interfaces.persistence.ReviewDao;
import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.swaps.Contact;
import ar.edu.itba.paw.model.swaps.SwapStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ReviewEligibilityService {
    private final ReviewDao reviewDao;

    public enum EligibilityStatus {
        ELIGIBLE,
        INVALID_STATUS,
        TRIP_NOT_FINISHED,
        NON_SWAP_OWNER
    }

    public ReviewEligibilityService(ReviewDao reviewDao) {
        this.reviewDao = reviewDao;
    }

    public EligibilityStatus getLeaveReviewEligibility(Contact contact, User user, LocalDate today) {
        if (contact.getStatus().compareTo(SwapStatus.ACCEPTED) != 0) {
            return EligibilityStatus.INVALID_STATUS;
        }

        DateRange reviewedStayRange = getReviewedStayRange(contact, user);
        if (reviewedStayRange == null || !reviewedStayRange.getEndDate().isBefore(today)) {
            return EligibilityStatus.TRIP_NOT_FINISHED;
        }

        if (!contact.isSwap() && user.getId() == contact.getRoomRequested().getOwner().getId()) {
            return EligibilityStatus.NON_SWAP_OWNER;
        }

        return EligibilityStatus.ELIGIBLE;
    }

    public boolean canLeaveReview(Contact contact, User user, LocalDate today) {
        return getLeaveReviewEligibility(contact, user, today) == EligibilityStatus.ELIGIBLE;
    }

    public boolean isReviewPending(Contact contact, User user, LocalDate today) {
        return canLeaveReview(contact, user, today)
                && !reviewDao.existsByContactAndReviewer(contact.getId(), user.getId());
    }

    private DateRange getReviewedStayRange(Contact contact, User user) {
        if (contact.isSwap() && contact.getRoomRequested().getOwner().getId() == user.getId()) {
            return contact.getOfferedRange();
        }
        return contact.getRequestedRange();
    }
}
