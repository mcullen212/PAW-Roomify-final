package ar.edu.itba.paw.webapp.security.helpers;

import ar.edu.itba.paw.interfaces.service.*;
import ar.edu.itba.paw.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class WebAuthHelper {
    private final RoomService roomService;
    private final ReviewService reviewService;
    private final ContactService contactService;
    private final GroupTripService groupTripService;
    private final UserService userService;

    public WebAuthHelper(RoomService roomService, ReviewService reviewService, ContactService contactService, GroupTripService groupTripService, UserService userService) {
        this.roomService = roomService;
        this.reviewService = reviewService;
        this.contactService = contactService;
        this.groupTripService = groupTripService;
        this.userService = userService;
    }

    private String getCurrentUserEmail(Authentication authentication) {
        if (authentication.getPrincipal() instanceof UserDetails) {
            return ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        return null;
    }

    public boolean isOwnerRoom(long roomId, Authentication authentication) {
        String email = getCurrentUserEmail(authentication);
        return roomService.isOwner(email, roomId);
    }

    public boolean isOwnerReview(long reviewId, Authentication authentication) {
        String email = getCurrentUserEmail(authentication);
        return reviewService.isOwner(email, reviewId);
    }

    public boolean isAccepter(long contactId, Authentication authentication) {
        String email = getCurrentUserEmail(authentication);
        return contactService.isAccepter(email, contactId);
    }

    public boolean belongsToSwap(long contactId, Authentication authentication) {
        String email = getCurrentUserEmail(authentication);
        return contactService.belongsToSwap(email,contactId);
    }

    public boolean isOwnerTrip(long groupTripId, Authentication authentication) {
        String email = getCurrentUserEmail(authentication);
        return groupTripService.isOwnerTrip(email, groupTripId);
    }

    public boolean isAuthorized(long userId, Authentication authentication) {
        String email = getCurrentUserEmail(authentication);
        if (email == null) return false;

        Optional<User> userOpt = userService.findUserByEmail(email);
        return userOpt.isPresent() && userOpt.get().getId() == userId;
    }
}
