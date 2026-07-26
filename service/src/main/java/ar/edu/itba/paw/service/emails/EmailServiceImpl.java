package ar.edu.itba.paw.service.emails;

import ar.edu.itba.paw.interfaces.service.EmailService;
import ar.edu.itba.paw.interfaces.service.MailContentBuilder;
import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.swaps.SwapStatus;
import ar.edu.itba.paw.model.token.VerificationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailServiceImpl implements EmailService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailServiceImpl.class);
    private final JavaMailSender mailSender;
    private final MailContentBuilder contentBuilder;
    private final MessageSource messageSource;

    public EmailServiceImpl(JavaMailSender mailSender, MailContentBuilder contentBuilder, MessageSource messageSource) {
        this.mailSender = mailSender;
        this.contentBuilder = contentBuilder;
        this.messageSource = messageSource;
    }

    private void sendEmail(String email, String subject, String bodyHtml) {
        try {
            MimeMessagePreparator messagePreparator = mimeMessage -> {
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setTo(email);
                helper.setSubject(subject);
                helper.setText(bodyHtml, true); // HTML
            };
            mailSender.send(messagePreparator);
            LOGGER.info("Sent email with subject {}", subject);
        } catch (MailException e) {
            LOGGER.error("Failed to send email with subject {}", subject, e);
            throw e;
        }
    }

    private void sendEmailTemplate(User recipient, String templateName, String subjectKey, Map<String, Object> vars) {
        String bodyContent = contentBuilder.build(templateName, vars, recipient.getLocale());

        Map<String, Object> baseVars = new HashMap<>();
        baseVars.put("body", bodyContent);
        String finalBody = contentBuilder.build("baseEmail", baseVars, recipient.getLocale());

        String subject = messageSource.getMessage(subjectKey, null, recipient.getLocale());
        LOGGER.info("Rendered bodyContent for template '{}'", templateName);

        sendEmail(recipient.getEmail(), subject, finalBody);
    }

    @Async
    @Override
    public void sendResetLink(User user, String token, String resetLink) {
        Map<String, Object> vars = Map.of(
                "userName", user.getName(),
                "token", token,
                "expiryHours", 1,
                "resetLink", resetLink
        );
        sendEmailTemplate(
                user,
                "resetLink",
                "email.reset.token.subject",
                vars
        );
    }

    @Async
    @Override
    public void sendSwapAcceptedOwner(User requester, User owner) {
        Map<String, Object> vars = Map.of(
                "userRequestName", requester.getName(),
                "emailRequester", requester.getEmail(),
                "userOwnerName", owner.getName(),
                "userOwnerEmail", owner.getEmail()
        );
        sendEmailTemplate(owner, "swapAcceptedOwner", "email.swapAccepted.subject", vars);
    }

    @Async
    @Override
    public void sendSwapAcceptedRequester(User requester, User owner) {
        Map<String, Object> vars = Map.of(
                "userRequestName", requester.getName(),
                "emailRequester", requester.getEmail(),
                "userOwnerName", owner.getName(),
                "userOwnerEmail", owner.getEmail()
        );
        sendEmailTemplate(requester, "swapAcceptedRequester", "email.swapAccepted.subject", vars);
    }

    @Async
    @Override
    public void sendVerifyEmail(User user, VerificationToken token, String verifyLink) {
        long hours = Duration.between(Instant.now(), token.getExpiryDate()).toHours();
        Map<String, Object> vars = Map.of(
                "userName", user.getName(),
                "verificationLink", verifyLink,
                "token", token.getToken(),
                "expiryHours", hours
        );
        sendEmailTemplate(
                user,
                "verifyEmail",
                "email.verify.subject",
                vars
        );
        LOGGER.warn("Verification email sent to user {}",user.getId());
    }

    @Async
    @Override
    public void sendSwapOwnerAcceptedOtherOffer(User requester, User owner, Room room, DateRange requestedRange) {
        Map<String, Object> vars = Map.of(
                "userRequestName", requester.getName(),
                "emailRequester", requester.getEmail(),
                "userOwnerName", owner.getName(),
                "userOwnerEmail", owner.getEmail(),
                "roomName", room.getTitle(),
                "city", room.getCity(),
                "country", room.getCountry(),
                "startDate", requestedRange.getStartDate(),
                "endDate", requestedRange.getEndDate()
        );
        sendEmailTemplate(requester, "swapRejectedOtherAccepted", "email.swapRejectedOtherAccepted.subject", vars);
    }

    @Async
    @Override
    public void sendSwapRejectedRequester(User requester,User owner, Room room) {
        Map<String, Object> vars = Map.of(
                "userRequestName", requester.getName(),
                "emailRequester", requester.getEmail(),
                "userOwnerName", owner.getName(),
                "userOwnerEmail", owner.getEmail(),
                "country", room.getCountry(),
                "roomName", room.getTitle()
        );
        sendEmailTemplate(requester, "swapRejected", "email.swapRejected.subject", vars);
    }

    @Async
    @Override
    public void sendSwapCancellationNotification(User userToNotify, User userCancelling, Room room, SwapStatus swapStatus) {
        Map<String, Object> vars = Map.of(
                "userToNotifyName", userToNotify.getName(),
                "emailToNotify", userToNotify.getEmail(),
                "userCancellingName", userCancelling.getName(),
                "userCancellingEmail", userCancelling.getEmail(),
                "country", room.getCountry(),
                "roomName", room.getTitle(),
                "swapStatus",swapStatus
        );
        sendEmailTemplate(userToNotify, "swapCancelled", "email.swapCancelled.subject", vars);
    }

    @Async
    @Override
    public void sendPendingContactRoomDeletedNotification(User userToNotify, String deletingUserName, String deletingUserEmail, String roomName, String country) {
        Map<String, Object> vars = Map.of(
                "userToNotifyName", userToNotify.getName(),
                "emailToNotify", userToNotify.getEmail(),
                "deletingUserName", deletingUserName,
                "deletingUserEmail", deletingUserEmail,
                "roomName", roomName,
                "country", country
        );
        sendEmailTemplate(userToNotify, "pendingContactRoomDeleted", "email.pendingContactRoomDeleted.subject", vars);
    }

    @Async
    @Override
    public void sendRoomUpdateNotification(User user, Map<String, Object> vars, String subjectKey) {
        Map<String, Object> fullVars = new HashMap<>(vars);
        fullVars.put("recipientName", user.getName() != null ? user.getName() : "User");
        sendEmailTemplate(user, "roomUpdated", subjectKey, fullVars);
    }

}
