package ar.edu.itba.paw.service;

import ar.edu.itba.paw.interfaces.service.MailContentBuilder;
import ar.edu.itba.paw.model.Image;
import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.rooms.BedType;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.rooms.RoomType;
import ar.edu.itba.paw.service.emails.EmailServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EmailServiceImplTest {

    private static final long REQUESTER_ID = 1L;
    private static final String REQUESTER_EMAIL = "tomasbalboak@gmail.com";
    private static final String REQUESTER_NAME = "Tomas";
    private static final long OWNER_ID = 2L;
    private static final String OWNER_EMAIL = "timmy@example.com";
    private static final String OWNER_NAME = "Timmy";
    private static final Locale LOCALE = Locale.ENGLISH;

    private final User requester = new User(REQUESTER_ID, REQUESTER_EMAIL, REQUESTER_NAME, "pass", true, LOCALE.toString(), null, null);
    private final User owner = new User(OWNER_ID, OWNER_EMAIL, OWNER_NAME, "pass", true, LOCALE.toString(), null, null);
    private final Image dummyImage = new Image(1L, "room.jpg", "image/jpeg", 3, new byte[]{1, 2, 3});
    private final Room requestedRoom = new Room(
            1L,
            "Requested Room",
            "AR",
            "Buenos Aires",
            "Description",
            RoomType.SHARED,
            BedType.TWIN,
            false,
            true,
            "Wifi",
            owner,
            dummyImage,
            BigDecimal.valueOf(100)
    );

    @Mock
    private JavaMailSender mockMailSender;

    @Mock
    private MailContentBuilder mockContentBuilder;

    @Mock
    private MessageSource mockMessageSource;

    @InjectMocks
    private EmailServiceImpl emailService;

    @Test
    public void testSendSwapRejectedRequester_UsesRequesterAsRecipientAndOwnerAsContact() {
        when(mockContentBuilder.build(eq("swapRejected"), anyMap(), eq(LOCALE))).thenReturn("body");
        when(mockContentBuilder.build(eq("baseEmail"), anyMap(), eq(LOCALE))).thenReturn("final body");
        when(mockMessageSource.getMessage(eq("email.swapRejected.subject"), isNull(), eq(LOCALE))).thenReturn("subject");

        emailService.sendSwapRejectedRequester(requester, owner, requestedRoom);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockContentBuilder).build(eq("swapRejected"), varsCaptor.capture(), eq(LOCALE));
        Map<String, Object> vars = varsCaptor.getValue();

        Assert.assertEquals(REQUESTER_NAME, vars.get("userRequestName"));
        Assert.assertEquals(REQUESTER_EMAIL, vars.get("emailRequester"));
        Assert.assertEquals(OWNER_NAME, vars.get("userOwnerName"));
        Assert.assertEquals(OWNER_EMAIL, vars.get("userOwnerEmail"));
        verify(mockMailSender).send(any(MimeMessagePreparator.class));
    }

    @Test
    public void testSendSwapOwnerAcceptedOtherOffer_UsesSpecificTemplateAndCompleteContext() {
        DateRange requestedRange = new DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5));
        when(mockContentBuilder.build(eq("swapRejectedOtherAccepted"), anyMap(), eq(LOCALE))).thenReturn("body");
        when(mockContentBuilder.build(eq("baseEmail"), anyMap(), eq(LOCALE))).thenReturn("final body");
        when(mockMessageSource.getMessage(eq("email.swapRejectedOtherAccepted.subject"), isNull(), eq(LOCALE))).thenReturn("subject");

        emailService.sendSwapOwnerAcceptedOtherOffer(requester, owner, requestedRoom, requestedRange);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockContentBuilder).build(eq("swapRejectedOtherAccepted"), varsCaptor.capture(), eq(LOCALE));
        Map<String, Object> vars = varsCaptor.getValue();

        Assert.assertEquals(REQUESTER_NAME, vars.get("userRequestName"));
        Assert.assertEquals(REQUESTER_EMAIL, vars.get("emailRequester"));
        Assert.assertEquals(OWNER_NAME, vars.get("userOwnerName"));
        Assert.assertEquals(OWNER_EMAIL, vars.get("userOwnerEmail"));
        Assert.assertEquals("Requested Room", vars.get("roomName"));
        Assert.assertEquals("Buenos Aires", vars.get("city"));
        Assert.assertEquals("AR", vars.get("country"));
        Assert.assertEquals(LocalDate.of(2026, 8, 1), vars.get("startDate"));
        Assert.assertEquals(LocalDate.of(2026, 8, 5), vars.get("endDate"));
        verify(mockMailSender).send(any(MimeMessagePreparator.class));
    }
}
