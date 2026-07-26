package ar.edu.itba.paw.service;

import ar.edu.itba.paw.interfaces.exceptions.BusinessException;
import ar.edu.itba.paw.interfaces.exceptions.ContactNotFoundException;
import ar.edu.itba.paw.interfaces.exceptions.ReviewNotFoundException;
import ar.edu.itba.paw.interfaces.persistence.ReviewDao;
import ar.edu.itba.paw.interfaces.service.ContactService;
import ar.edu.itba.paw.interfaces.service.UserService;
import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.DTO.ReviewPageDTO;
import ar.edu.itba.paw.model.rooms.*;
import ar.edu.itba.paw.model.swaps.Contact;
import ar.edu.itba.paw.model.swaps.SwapStatus;
import ar.edu.itba.paw.service.reviews.ReviewEligibilityService;
import ar.edu.itba.paw.service.reviews.ReviewServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.MessageSource;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReviewServiceImplTest {
    private static final long CONTACT_ID = 5L;
    private static final long REVIEWER_ID = 1L;
    private static final long OWNER_ID = 2L;
    private static final long ROOM_ID = 10L;
    private static final long REVIEW_ID = 99L;
    private static final String REVIEWER_EMAIL = "reviewer@test.com";
    private static final double RATING = 4.5;
    private static final String COMMENT = "Great stay!";
    private static final Locale LOCALE = Locale.ENGLISH;
    private static final SwapStatus STATUS_ACCEPTED = SwapStatus.ACCEPTED;
    private static final SwapStatus STATUS_PENDING = SwapStatus.PENDING;
    private static final String TITLE = "Cozy Apartment";
    private static final BigDecimal PRICE_DAY = BigDecimal.valueOf(50);
    private static final long IMAGE_ID = 1L;

    @Mock
    private ReviewDao mockReviewDao;
    @Mock
    private ContactService mockContactService;
    @Mock
    private UserService mockUserService;
    @Mock
    private MessageSource mockMessageSource;
    @Mock
    private ReviewEligibilityService mockReviewEligibilityService;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private final User reviewerUser = new User(REVIEWER_ID, REVIEWER_EMAIL, "Rev", "pass", false, LOCALE.toString(), null, null);
    private final User ownerUser = new User(OWNER_ID, "owner@test.com", "Own", "pass", false, LOCALE.toString(), null, null);
    private final Image dummyImage = new Image(IMAGE_ID, "image.jpg", "image/jpeg", 3, new byte[]{1, 2, 3});
    private final Room roomRequested = new Room(TITLE,"Title", "AR", "BA", RoomType.PRIVATE, BedType.KING, true, false, Amenity.GYM.toString(), ownerUser,dummyImage, PRICE_DAY);

    // DateRange: Past date (required for successful review)
    private final DateRange pastRange = new DateRange(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5));
    // DateRange: Future date (used to fail the date check)
    private final DateRange futureRange = new DateRange(LocalDate.now().plusDays(5), LocalDate.now().plusDays(10));

    // Contacts
    private final Contact acceptedPastMoneyContact = new Contact(CONTACT_ID, roomRequested, LocalDateTime.now(), STATUS_ACCEPTED, false, BigDecimal.TEN, ownerUser, null, pastRange, null);
    private final Contact acceptedFutureSwapContact = new Contact(CONTACT_ID + 1, roomRequested, LocalDateTime.now(), STATUS_ACCEPTED, true, null, ownerUser, null, futureRange, null);
    private final Contact pendingPastContact = new Contact(CONTACT_ID + 2, roomRequested, LocalDateTime.now(), STATUS_PENDING, false, BigDecimal.TEN, ownerUser, null, pastRange, null);

    @Test
    public void testAddReview_Successful() throws BusinessException {
        // 1. Setup Mocks
        final Review expectedReview = new Review(1L, acceptedPastMoneyContact, reviewerUser, RATING, COMMENT, LocalDateTime.now());

        when(mockUserService.findUserById(REVIEWER_ID)).thenReturn(Optional.of(reviewerUser));
        when(mockContactService.getContactById(CONTACT_ID)).thenReturn(Optional.of(acceptedPastMoneyContact));
        when(mockReviewEligibilityService.getLeaveReviewEligibility(eq(acceptedPastMoneyContact), eq(reviewerUser), any(LocalDate.class)))
                .thenReturn(ReviewEligibilityService.EligibilityStatus.ELIGIBLE);
        when(mockReviewDao.existsByContactAndReviewer(CONTACT_ID, REVIEWER_ID)).thenReturn(false);
        when(mockReviewDao.save(eq(acceptedPastMoneyContact), eq(reviewerUser), eq(RATING), eq(COMMENT), any(LocalDateTime.class)))
                .thenReturn(expectedReview);

        // 2. Exercise
        Review result = reviewService.addReview(CONTACT_ID, REVIEWER_ID, RATING, COMMENT, LOCALE);

        // 3. Assertions
        Assert.assertNotNull(result);
        Assert.assertEquals(RATING, result.getRating(), 0.001);
        Assert.assertEquals(COMMENT, result.getComment());
        Assert.assertEquals(REVIEWER_ID, result.getReviewer().getId());
    }

    @Test
    public void testAddReview_UserNotFound() {
        // 1. Setup Mocks
        when(mockUserService.findUserById(REVIEWER_ID)).thenReturn(Optional.empty());

        // 2. Exercise
        assertThrows(
                UsernameNotFoundException.class,
                () -> reviewService.addReview(CONTACT_ID, REVIEWER_ID, RATING, COMMENT, LOCALE)
        );
    }

    @Test
    public void testAddReview_ContactNotFound() {
        // 1. Setup Mocks
        when(mockUserService.findUserById(REVIEWER_ID)).thenReturn(Optional.of(reviewerUser));
        when(mockContactService.getContactById(CONTACT_ID)).thenReturn(Optional.empty());

        // 2. Exercise
        assertThrows(
                ContactNotFoundException.class,
                () -> reviewService.addReview(CONTACT_ID, REVIEWER_ID, RATING, COMMENT, LOCALE)
        );
    }

    @Test
    public void testAddReview_ContactNotAccepted() {
        // 1. Setup Mocks (Contact is PENDING)
        when(mockUserService.findUserById(REVIEWER_ID)).thenReturn(Optional.of(reviewerUser));
        when(mockContactService.getContactById(CONTACT_ID)).thenReturn(Optional.of(pendingPastContact)); // Status PENDING
        when(mockReviewEligibilityService.getLeaveReviewEligibility(eq(pendingPastContact), eq(reviewerUser), any(LocalDate.class)))
                .thenReturn(ReviewEligibilityService.EligibilityStatus.INVALID_STATUS);
        when(mockMessageSource.getMessage(eq("review.error.cannot.review.status"), any(), any())).thenReturn("Contact not accepted");

        // 2. Exercise
        assertThrows(
                BusinessException.class,
                () -> reviewService.addReview(CONTACT_ID, REVIEWER_ID, RATING, COMMENT, LOCALE)
        );
    }

    @Test
    public void testAddReview_TripNotInPast() {
        // 1. Setup Mocks (Contact end date is in the future)
        when(mockUserService.findUserById(REVIEWER_ID)).thenReturn(Optional.of(reviewerUser));
        when(mockContactService.getContactById(anyLong())).thenReturn(Optional.of(acceptedFutureSwapContact)); // EndDate is Future
        when(mockReviewEligibilityService.getLeaveReviewEligibility(eq(acceptedFutureSwapContact), eq(reviewerUser), any(LocalDate.class)))
                .thenReturn(ReviewEligibilityService.EligibilityStatus.TRIP_NOT_FINISHED);
        when(mockMessageSource.getMessage(eq("review.error.cannot.review.date"), any(), any())).thenReturn("Trip not completed");

        // 2. Exercise
        assertThrows(
                BusinessException.class,
                () -> reviewService.addReview(CONTACT_ID, REVIEWER_ID, RATING, COMMENT, LOCALE)
        );
    }

    @Test
    public void testAddReview_ReviewAlreadyExists() {
        // 1. Setup Mocks
        when(mockUserService.findUserById(REVIEWER_ID)).thenReturn(Optional.of(reviewerUser));
        when(mockContactService.getContactById(CONTACT_ID)).thenReturn(Optional.of(acceptedPastMoneyContact));
        when(mockReviewEligibilityService.getLeaveReviewEligibility(eq(acceptedPastMoneyContact), eq(reviewerUser), any(LocalDate.class)))
                .thenReturn(ReviewEligibilityService.EligibilityStatus.ELIGIBLE);
        when(mockReviewDao.existsByContactAndReviewer(CONTACT_ID, REVIEWER_ID)).thenReturn(true); // Fails here
        when(mockMessageSource.getMessage(eq("review.error.already.exists"), any(), any())).thenReturn("Review already exists");

        // 2. Exercise
        assertThrows(
                BusinessException.class,
                () -> reviewService.addReview(CONTACT_ID, REVIEWER_ID, RATING, COMMENT, LOCALE)
        );
    }

    @Test
    public void testIsReviewPending_True() {
        // 1. Setup Mocks (All conditions must pass)
        when(mockUserService.findUserByEmail(REVIEWER_EMAIL)).thenReturn(Optional.of(reviewerUser));
        when(mockContactService.getContactById(CONTACT_ID)).thenReturn(Optional.of(acceptedPastMoneyContact));
        when(mockReviewEligibilityService.isReviewPending(eq(acceptedPastMoneyContact), eq(reviewerUser), any(LocalDate.class))).thenReturn(true);

        // 2. Exercise
        boolean result = reviewService.isReviewPending(CONTACT_ID, REVIEWER_EMAIL);

        // 3. Assertions
        Assert.assertTrue(result);
    }

    @Test
    public void testIsReviewPending_False_ContactNotFound() {
        // 1. Setup Mocks
        when(mockUserService.findUserByEmail(REVIEWER_EMAIL)).thenReturn(Optional.of(reviewerUser));
        when(mockContactService.getContactById(CONTACT_ID)).thenReturn(Optional.empty()); // Fails here

        // 2. Exercise
        boolean result = reviewService.isReviewPending(CONTACT_ID, REVIEWER_EMAIL);

        // 3. Assertions
        Assert.assertFalse(result);
    }

    @Test
    public void testIsReviewPending_False_NotAccepted() {
        // 1. Setup Mocks
        when(mockUserService.findUserByEmail(REVIEWER_EMAIL)).thenReturn(Optional.of(reviewerUser));
        when(mockContactService.getContactById(CONTACT_ID)).thenReturn(Optional.of(pendingPastContact)); // Status PENDING

        // 2. Exercise
        boolean result = reviewService.isReviewPending(CONTACT_ID, REVIEWER_EMAIL);

        // 3. Assertions
        Assert.assertFalse(result);
    }

    @Test
    public void testIsReviewPending_False_TripNotInPast() {
        // 1. Setup Mocks
        when(mockUserService.findUserByEmail(REVIEWER_EMAIL)).thenReturn(Optional.of(reviewerUser));
        when(mockContactService.getContactById(CONTACT_ID + 1)).thenReturn(Optional.of(acceptedFutureSwapContact)); // End date in future

        // 2. Exercise
        boolean result = reviewService.isReviewPending(CONTACT_ID + 1, REVIEWER_EMAIL);

        // 3. Assertions
        Assert.assertFalse(result);
    }

    @Test
    public void testIsReviewPending_False_ReviewAlreadyExists() {
        // 1. Setup Mocks
        when(mockUserService.findUserByEmail(REVIEWER_EMAIL)).thenReturn(Optional.of(reviewerUser));
        when(mockContactService.getContactById(CONTACT_ID)).thenReturn(Optional.of(acceptedPastMoneyContact));

        // 2. Exercise
        boolean result = reviewService.isReviewPending(CONTACT_ID, REVIEWER_EMAIL);

        // 3. Assertions
        Assert.assertFalse(result);
    }

    @Test
    public void testIsReviewPending_False_NonSwapOwnerCannotReview() {
        // 1. Setup Mocks (Reviewer is the OWNER of the requested room in a NON-SWAP)
        final Contact nonSwapOwned = new Contact(CONTACT_ID, roomRequested, LocalDateTime.now(), STATUS_ACCEPTED, false, BigDecimal.TEN, ownerUser, null, pastRange, null);

        when(mockUserService.findUserByEmail(ownerUser.getEmail())).thenReturn(Optional.of(ownerUser));
        when(mockContactService.getContactById(CONTACT_ID)).thenReturn(Optional.of(nonSwapOwned));

        // 2. Exercise (The owner's email is passed)
        boolean result = reviewService.isReviewPending(CONTACT_ID, ownerUser.getEmail());

        // 3. Assertions
        Assert.assertFalse(result); // Expected to be false because owner cannot review their own non-swap room
    }

    @Test
    public void testGetRoomRating_Successful() {
        // 1. Setup Mocks
        int expectedRating = 4;
        when(mockReviewDao.roomRating(ROOM_ID)).thenReturn((double) expectedRating);

        // 2. Exercise
        double result = reviewService.getRoomRating(ROOM_ID);

        // 3. Assertions
        Assert.assertEquals(expectedRating, result, 0.001);
    }

    @Test
    public void testGetReviewsPage_ByRoomId() {
        final int page = 1;
        final int pageSize = 12;
        final int totalReviews = 25;
        final Review review = new Review(1L, acceptedPastMoneyContact, reviewerUser, RATING, COMMENT, LocalDateTime.now());
        final List<Review> expectedReviews = Collections.singletonList(review);

        when(mockReviewDao.findByRoomId(ROOM_ID, page, pageSize)).thenReturn(expectedReviews);
        when(mockReviewDao.getReviewsCountByRoomId(ROOM_ID)).thenReturn(totalReviews);

        ReviewPageDTO result = reviewService.getReviewsPage(ROOM_ID, null, null, page, pageSize);

        Assert.assertEquals(expectedReviews, result.getReviews());
        Assert.assertEquals(totalReviews, result.getTotalReviews());
        Assert.assertEquals(3, result.getTotalPages());
    }

    @Test
    public void testGetReviewsPage_ByUserId() {
        final int page = 2;
        final int pageSize = 10;
        final int totalReviews = 11;
        final Review review = new Review(1L, acceptedPastMoneyContact, reviewerUser, RATING, COMMENT, LocalDateTime.now());
        final List<Review> expectedReviews = Collections.singletonList(review);

        when(mockReviewDao.findByReviewerId(REVIEWER_ID, page, pageSize)).thenReturn(expectedReviews);
        when(mockReviewDao.countByReviewerId(REVIEWER_ID)).thenReturn(totalReviews);

        ReviewPageDTO result = reviewService.getReviewsPage(null, REVIEWER_ID, null, page, pageSize);

        Assert.assertEquals(expectedReviews, result.getReviews());
        Assert.assertEquals(totalReviews, result.getTotalReviews());
        Assert.assertEquals(2, result.getTotalPages());
    }

    @Test
    public void testGetReviewsPage_ClampsPagePastLastPage() {
        final int requestedPage = 4;
        final int lastPage = 3;
        final int pageSize = 4;
        final int totalReviews = 12;
        final Review review = new Review(1L, acceptedPastMoneyContact, reviewerUser, RATING, COMMENT, LocalDateTime.now());
        final List<Review> expectedReviews = Collections.singletonList(review);

        when(mockReviewDao.getReviewsCountByRoomId(ROOM_ID)).thenReturn(totalReviews);
        when(mockReviewDao.findByRoomId(ROOM_ID, lastPage, pageSize)).thenReturn(expectedReviews);

        ReviewPageDTO result = reviewService.getReviewsPage(ROOM_ID, null, null, requestedPage, pageSize);

        Assert.assertEquals(expectedReviews, result.getReviews());
        Assert.assertEquals(totalReviews, result.getTotalReviews());
        Assert.assertEquals(3, result.getTotalPages());
    }

    @Test
    public void testGetReviewsPage_ByRoomOwnerId() {
        final int page = 1;
        final int pageSize = 4;
        final int totalReviews = 9;
        final Review review = new Review(1L, acceptedPastMoneyContact, reviewerUser, RATING, COMMENT, LocalDateTime.now());
        final List<Review> expectedReviews = Collections.singletonList(review);

        when(mockReviewDao.countReviewsByRoomOwner(OWNER_ID)).thenReturn(totalReviews);
        when(mockReviewDao.getReviewsByRoomOfOwner(OWNER_ID, page, pageSize)).thenReturn(expectedReviews);

        ReviewPageDTO result = reviewService.getReviewsPage(null, null, OWNER_ID, page, pageSize);

        Assert.assertEquals(expectedReviews, result.getReviews());
        Assert.assertEquals(totalReviews, result.getTotalReviews());
        Assert.assertEquals(3, result.getTotalPages());
    }

    @Test
    public void testGetReviewsPage_MissingFilter() {
        assertThrows(
                IllegalArgumentException.class,
                () -> reviewService.getReviewsPage(null, null, null, 1, 12)
        );
    }

    @Test
    public void testDeleteReview_Successful() {
        when(mockReviewDao.deleteReview(REVIEW_ID)).thenReturn(true);

        reviewService.deleteReview(REVIEW_ID);

        verify(mockReviewDao).deleteReview(REVIEW_ID);
    }

    @Test
    public void testDeleteReview_MissingReviewThrowsNotFound() {
        when(mockReviewDao.deleteReview(REVIEW_ID)).thenReturn(false);

        assertThrows(
                ReviewNotFoundException.class,
                () -> reviewService.deleteReview(REVIEW_ID)
        );

        verify(mockReviewDao).deleteReview(REVIEW_ID);
    }

    @Test
    public void testIsOwner_ForeignReviewReturnsFalse() {
        final Review review = new Review(REVIEW_ID, acceptedPastMoneyContact, ownerUser, RATING, COMMENT, LocalDateTime.now());

        when(mockUserService.findUserByEmail(REVIEWER_EMAIL)).thenReturn(Optional.of(reviewerUser));
        when(mockReviewDao.findReviewById(REVIEW_ID)).thenReturn(Optional.of(review));

        Assert.assertFalse(reviewService.isOwner(REVIEWER_EMAIL, REVIEW_ID));

        verify(mockReviewDao, never()).deleteReview(REVIEW_ID);
    }

    @Test
    public void testIsOwner_OwnReviewReturnsTrue() {
        final Review review = new Review(REVIEW_ID, acceptedPastMoneyContact, reviewerUser, RATING, COMMENT, LocalDateTime.now());

        when(mockUserService.findUserByEmail(REVIEWER_EMAIL)).thenReturn(Optional.of(reviewerUser));
        when(mockReviewDao.findReviewById(REVIEW_ID)).thenReturn(Optional.of(review));

        Assert.assertTrue(reviewService.isOwner(REVIEWER_EMAIL, REVIEW_ID));

        verify(mockReviewDao, never()).deleteReview(REVIEW_ID);
    }

    @Test
    public void testIsOwner_MissingReviewThrowsNotFound() {
        when(mockUserService.findUserByEmail(REVIEWER_EMAIL)).thenReturn(Optional.of(reviewerUser));
        when(mockReviewDao.findReviewById(REVIEW_ID)).thenReturn(Optional.empty());

        assertThrows(
                ReviewNotFoundException.class,
                () -> reviewService.isOwner(REVIEWER_EMAIL, REVIEW_ID)
        );

        verify(mockReviewDao, never()).deleteReview(REVIEW_ID);
    }

}
