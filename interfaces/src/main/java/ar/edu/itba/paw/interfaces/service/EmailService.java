package ar.edu.itba.paw.interfaces.service;

import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.swaps.SwapStatus;
import ar.edu.itba.paw.model.token.VerificationToken;

import java.util.Map;

public interface EmailService {
    void sendResetLink(User user, String token, String resetLink);
    void sendSwapAcceptedOwner(User requester, User owner);
    void sendSwapAcceptedRequester(User requester, User owner);
    void sendVerifyEmail(User user, VerificationToken token, String verifyLink);
    void sendSwapOwnerAcceptedOtherOffer(User requester, User owner, Room room, DateRange requestedRange);
    void sendSwapRejectedRequester(User requester,User owner, Room room);
    void sendSwapCancellationNotification(User userToNotify, User userCancelling, Room room, SwapStatus swapStatus);
    void sendPendingContactRoomDeletedNotification(User userToNotify, String deletingUserName, String deletingUserEmail, String roomName, String country);
    void sendRoomUpdateNotification(User user, Map<String, Object> vars, String subjectKey);

}
