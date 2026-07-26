package ar.edu.itba.paw.service;

import ar.edu.itba.paw.interfaces.exceptions.UserNotFoundException;
import ar.edu.itba.paw.interfaces.service.ContactService;
import ar.edu.itba.paw.interfaces.service.ReviewService;
import ar.edu.itba.paw.interfaces.service.RoomService;
import ar.edu.itba.paw.interfaces.service.UserService;
import ar.edu.itba.paw.model.DTO.PublicUserProfileStats;
import ar.edu.itba.paw.model.DTO.UserProfileStats;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.service.users.ProfileServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

import static org.junit.Assert.assertThrows;

@RunWith(MockitoJUnitRunner.class)
public class ProfileServiceImplTest {

    private static final long USER_ID = 1L;
    private static final String EMAIL = "user@example.com";
    private static final String NAME = "name";
    private static final String PASSWORD = "encodedPassword";
    private static final Locale LOCALE = Locale.ENGLISH;
    private static final long TOTAL_REVIEWS = 3L;
    private static final long TOTAL_WRITTEN_REVIEWS = 4L;
    private static final double REVIEW_AVG = 4.5;
    private static final BigDecimal TOTAL_EARNED = BigDecimal.valueOf(100);
    private static final BigDecimal TOTAL_SPENT = BigDecimal.valueOf(75);
    private static final long TOTAL_SWAPS = 5L;
    private static final int TOTAL_ROOMS = 2;
    private static final double DOUBLE_DELTA = 0.001;

    @Mock
    private UserService mockUserService;

    @Mock
    private ReviewService mockReviewService;

    @Mock
    private ContactService mockContactService;

    @Mock
    private RoomService mockRoomService;

    @InjectMocks
    private ProfileServiceImpl profileService;

    @Test
    public void testGetPrivateProfile_UserExists_ReturnsProfileStats() {
        User user = user(USER_ID, EMAIL);
        Mockito.when(mockUserService.findUserById(USER_ID)).thenReturn(Optional.of(user));
        Mockito.when(mockReviewService.countReviewsByRoomOwnerId(USER_ID)).thenReturn((int) TOTAL_REVIEWS);
        Mockito.when(mockReviewService.countReviewsByUserId(USER_ID)).thenReturn((int) TOTAL_WRITTEN_REVIEWS);
        Mockito.when(mockReviewService.getAverageRatingByRoomOwnerId(USER_ID)).thenReturn(REVIEW_AVG);
        Mockito.when(mockContactService.getTotalMoneyEarnedByUser(USER_ID)).thenReturn(TOTAL_EARNED);
        Mockito.when(mockContactService.getTotalMoneySpentByUser(USER_ID)).thenReturn(TOTAL_SPENT);
        Mockito.when(mockContactService.countCompletedSwapsByUser(USER_ID)).thenReturn(TOTAL_SWAPS);

        UserProfileStats result = profileService.getPrivateProfile(USER_ID);

        Assert.assertEquals(user, result.getUser());
        Assert.assertEquals(TOTAL_REVIEWS, result.getTotalReviews());
        Assert.assertEquals(TOTAL_WRITTEN_REVIEWS, result.getTotalWrittenReviews());
        Assert.assertEquals(REVIEW_AVG, result.getReviewAvg(), DOUBLE_DELTA);
        Assert.assertEquals(TOTAL_EARNED, result.getTotalEarned());
        Assert.assertEquals(TOTAL_SPENT, result.getTotalSpent());
        Assert.assertEquals(TOTAL_SWAPS, result.getTotalSwaps());
        Mockito.verify(mockReviewService).countReviewsByRoomOwnerId(USER_ID);
        Mockito.verify(mockReviewService).countReviewsByUserId(USER_ID);
        Mockito.verify(mockReviewService).getAverageRatingByRoomOwnerId(USER_ID);
        Mockito.verify(mockContactService).getTotalMoneyEarnedByUser(USER_ID);
        Mockito.verify(mockContactService).getTotalMoneySpentByUser(USER_ID);
        Mockito.verify(mockContactService).countCompletedSwapsByUser(USER_ID);
    }

    @Test
    public void testGetPrivateProfile_UserDoesNotExist() {
        Mockito.when(mockUserService.findUserById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(
            UserNotFoundException.class,
            () -> profileService.getPrivateProfile(USER_ID)
        );
    }

    @Test
    public void testGetPublicProfile_UserExists_ReturnsPublicProfileStats() {
        User user = user(USER_ID, EMAIL);
        Mockito.when(mockUserService.findUserById(USER_ID)).thenReturn(Optional.of(user));
        Mockito.when(mockReviewService.countReviewsByRoomOwner(USER_ID)).thenReturn((int) TOTAL_REVIEWS);
        Mockito.when(mockReviewService.getAverageRatingByRoomOwner(USER_ID)).thenReturn(REVIEW_AVG);
        Mockito.when(mockRoomService.countRoomsByOwnerId(USER_ID)).thenReturn(TOTAL_ROOMS);

        PublicUserProfileStats result = profileService.getPublicProfile(USER_ID);

        Assert.assertEquals(user, result.getUser());
        Assert.assertEquals(TOTAL_REVIEWS, result.getTotalReviews());
        Assert.assertEquals(REVIEW_AVG, result.getAverageRating(), DOUBLE_DELTA);
        Assert.assertEquals(TOTAL_ROOMS, result.getTotalRooms());
        Mockito.verify(mockReviewService).countReviewsByRoomOwner(USER_ID);
        Mockito.verify(mockReviewService).getAverageRatingByRoomOwner(USER_ID);
        Mockito.verify(mockRoomService).countRoomsByOwnerId(USER_ID);
    }

    @Test
    public void testGetPublicProfile_UserDoesNotExist() {
        Mockito.when(mockUserService.findUserById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(
            UserNotFoundException.class,
            () -> profileService.getPublicProfile(USER_ID)
        );
    }

    private User user(long id, String email) {
        return new User(
            id,
            email,
            NAME,
            PASSWORD,
            false,
            LOCALE.toString(),
            null,
            null
        );
    }
}
