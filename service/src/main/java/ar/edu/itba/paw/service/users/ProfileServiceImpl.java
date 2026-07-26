package ar.edu.itba.paw.service.users;

import ar.edu.itba.paw.interfaces.exceptions.UserNotFoundException;
import ar.edu.itba.paw.interfaces.service.ContactService;
import ar.edu.itba.paw.interfaces.service.ProfileService;
import ar.edu.itba.paw.interfaces.service.ReviewService;
import ar.edu.itba.paw.interfaces.service.RoomService;
import ar.edu.itba.paw.interfaces.service.UserService;
import ar.edu.itba.paw.model.DTO.PublicUserProfileStats;
import ar.edu.itba.paw.model.DTO.UserProfileStats;
import ar.edu.itba.paw.model.User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final UserService userService;
    private final ReviewService reviewService;
    private final ContactService contactService;
    private final RoomService roomService;

    public ProfileServiceImpl(
        UserService userService,
        ReviewService reviewService,
        ContactService contactService,
        RoomService roomService
    ) {
        this.userService = userService;
        this.reviewService = reviewService;
        this.contactService = contactService;
        this.roomService = roomService;
    }

    @Override
    public UserProfileStats getPrivateProfile(long userId) {
        User user = userService.findUserById(userId).orElseThrow(() ->
            new UserNotFoundException("User not found")
        );

        long totalReviews = reviewService.countReviewsByRoomOwnerId(userId);
        long totalWrittenReviews = reviewService.countReviewsByUserId(userId);
        double reviewAvg = reviewService.getAverageRatingByRoomOwnerId(userId);
        BigDecimal totalEarned = contactService.getTotalMoneyEarnedByUser(userId);
        BigDecimal totalSpent = contactService.getTotalMoneySpentByUser(userId);
        long totalSwaps = contactService.countCompletedSwapsByUser(userId);

        return new UserProfileStats(
            user,
            totalReviews,
            totalWrittenReviews,
            reviewAvg,
            totalEarned,
            totalSpent,
            totalSwaps
        );
    }

    @Override
    public PublicUserProfileStats getPublicProfile(long userId) {
        User user = userService.findUserById(userId).orElseThrow(() ->
            new UserNotFoundException("User not found")
        );

        long totalReviews = reviewService.countReviewsByRoomOwner(user.getId());
        double averageRating = reviewService.getAverageRatingByRoomOwner(user.getId());
        int totalRooms = roomService.countRoomsByOwnerId(user.getId());

        return new PublicUserProfileStats(
            user,
            totalReviews,
            averageRating,
            totalRooms
        );
    }
}
