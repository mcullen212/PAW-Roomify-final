package ar.edu.itba.paw.service;

import ar.edu.itba.paw.interfaces.persistence.ReviewDao;
import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.Image;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.rooms.BedType;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.rooms.RoomType;
import ar.edu.itba.paw.model.swaps.Contact;
import ar.edu.itba.paw.model.swaps.SwapStatus;
import ar.edu.itba.paw.service.reviews.ReviewEligibilityService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReviewEligibilityServiceTest {
    private static final long CONTACT_ID = 5L;
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 19);
    private static final DateRange PAST_RANGE = new DateRange(TODAY.minusDays(7), TODAY.minusDays(1));
    private static final DateRange FUTURE_RANGE = new DateRange(TODAY.plusDays(1), TODAY.plusDays(5));

    @Mock
    private ReviewDao mockReviewDao;

    private final User requestedOwner = new User(1L, "owner@test.com", "Owner", "pass", false, Locale.ENGLISH.toString(), null, null);
    private final User requester = new User(2L, "requester@test.com", "Requester", "pass", false, Locale.ENGLISH.toString(), null, null);
    private final Image image = new Image(1L, "image.jpg", "image/jpeg", 3, new byte[]{1, 2, 3});
    private final Room requestedRoom = new Room("Requested", "Desc", "AR", "BA", RoomType.PRIVATE, BedType.TWIN, false, false, "", requestedOwner, image, BigDecimal.TEN);
    private final Room offeredRoom = new Room("Offered", "Desc", "AR", "BA", RoomType.PRIVATE, BedType.TWIN, false, false, "", requester, image, BigDecimal.TEN);

    private ReviewEligibilityService reviewEligibilityService;

    @Before
    public void setUp() {
        reviewEligibilityService = new ReviewEligibilityService(mockReviewDao);
    }

    @Test
    public void testMoneyStayPendingReviewAfterRequestedRangeFinished() {
        Contact contact = moneyContact(SwapStatus.ACCEPTED, PAST_RANGE);
        when(mockReviewDao.existsByContactAndReviewer(CONTACT_ID, requester.getId())).thenReturn(false);

        Assert.assertTrue(reviewEligibilityService.isReviewPending(contact, requester, TODAY));
    }

    @Test
    public void testMoneyStayNotPendingReviewBeforeRequestedRangeFinished() {
        Contact contact = moneyContact(SwapStatus.ACCEPTED, FUTURE_RANGE);

        Assert.assertFalse(reviewEligibilityService.isReviewPending(contact, requester, TODAY));
    }

    @Test
    public void testSwapRequesterUsesRequestedRange() {
        Contact contact = swapContact(PAST_RANGE, FUTURE_RANGE);
        when(mockReviewDao.existsByContactAndReviewer(CONTACT_ID, requester.getId())).thenReturn(false);

        Assert.assertTrue(reviewEligibilityService.isReviewPending(contact, requester, TODAY));
    }

    @Test
    public void testRequestedRoomOwnerUsesOfferedRange() {
        Contact contact = swapContact(PAST_RANGE, FUTURE_RANGE);

        Assert.assertFalse(reviewEligibilityService.isReviewPending(contact, requestedOwner, TODAY));
    }

    @Test
    public void testRequestedRoomOwnerCanReviewAfterOfferedRangeFinished() {
        Contact contact = swapContact(FUTURE_RANGE, PAST_RANGE);
        when(mockReviewDao.existsByContactAndReviewer(CONTACT_ID, requestedOwner.getId())).thenReturn(false);

        Assert.assertTrue(reviewEligibilityService.isReviewPending(contact, requestedOwner, TODAY));
    }

    @Test
    public void testAlreadyReviewedContactIsNotPending() {
        Contact contact = moneyContact(SwapStatus.ACCEPTED, PAST_RANGE);
        when(mockReviewDao.existsByContactAndReviewer(CONTACT_ID, requester.getId())).thenReturn(true);

        Assert.assertFalse(reviewEligibilityService.isReviewPending(contact, requester, TODAY));
    }

    @Test
    public void testNonAcceptedContactIsNotPending() {
        Contact contact = moneyContact(SwapStatus.PENDING, PAST_RANGE);

        Assert.assertFalse(reviewEligibilityService.isReviewPending(contact, requester, TODAY));
    }

    @Test
    public void testNonSwapOwnerCannotReviewOwnRoom() {
        Contact contact = moneyContact(SwapStatus.ACCEPTED, PAST_RANGE);

        Assert.assertEquals(
                ReviewEligibilityService.EligibilityStatus.NON_SWAP_OWNER,
                reviewEligibilityService.getLeaveReviewEligibility(contact, requestedOwner, TODAY)
        );
    }

    private Contact moneyContact(SwapStatus status, DateRange requestedRange) {
        return new Contact(
                CONTACT_ID,
                requestedRoom,
                LocalDateTime.now(),
                status,
                false,
                BigDecimal.TEN,
                requester,
                null,
                requestedRange,
                null
        );
    }

    private Contact swapContact(DateRange requestedRange, DateRange offeredRange) {
        return new Contact(
                CONTACT_ID,
                requestedRoom,
                LocalDateTime.now(),
                SwapStatus.ACCEPTED,
                true,
                null,
                requester,
                offeredRoom,
                requestedRange,
                offeredRange
        );
    }
}
