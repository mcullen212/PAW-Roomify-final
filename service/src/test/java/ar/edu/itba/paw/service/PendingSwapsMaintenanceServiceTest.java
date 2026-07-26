package ar.edu.itba.paw.service;

import ar.edu.itba.paw.interfaces.persistence.ContactDao;
import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.Image;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.rooms.BedType;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.rooms.RoomType;
import ar.edu.itba.paw.model.swaps.Contact;
import ar.edu.itba.paw.model.swaps.SwapStatus;
import ar.edu.itba.paw.service.contacts.PendingSwapsMaintenanceService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PendingSwapsMaintenanceServiceTest {
    private static final long CONTACT_ID = 10L;
    private static final long SECOND_CONTACT_ID = 11L;

    @Mock
    private ContactDao mockContactDao;

    @InjectMocks
    private PendingSwapsMaintenanceService pendingSwapsMaintenanceService;

    @Test
    public void testUpdateExpiredTripStatuses_IsScheduledEveryDayAtTwoAm() throws NoSuchMethodException {
        Method method = PendingSwapsMaintenanceService.class.getMethod("updateExpiredTripStatuses");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        Assert.assertNotNull(scheduled);
        Assert.assertEquals("0 0 2 * * *", scheduled.cron());
        Assert.assertEquals("America/Argentina/Buenos_Aires", scheduled.zone());
    }

    @Test
    public void testUpdateExpiredTripStatuses_ExpiredPendingSwaps_MarksThemExpired() {
        Contact firstContact = newContact(CONTACT_ID);
        Contact secondContact = newContact(SECOND_CONTACT_ID);
        when(mockContactDao.findExpiredPendingSwaps()).thenReturn(List.of(firstContact, secondContact));

        pendingSwapsMaintenanceService.updateExpiredTripStatuses();

        verify(mockContactDao).updateStatus(CONTACT_ID, SwapStatus.EXPIRED);
        verify(mockContactDao).updateStatus(SECOND_CONTACT_ID, SwapStatus.EXPIRED);
    }

    @Test
    public void testUpdateExpiredTripStatuses_NoExpiredPendingSwaps_DoesNotUpdate() {
        when(mockContactDao.findExpiredPendingSwaps()).thenReturn(List.of());

        pendingSwapsMaintenanceService.updateExpiredTripStatuses();

        verify(mockContactDao, never()).updateStatus(anyLong(), any());
    }

    private Contact newContact(long contactId) {
        User owner = new User(1L, "owner@test.com", "Owner", "pass", true, "en", null, null);
        User requester = new User(2L, "requester@test.com", "Requester", "pass", true, "en", null, null);
        Image image = new Image(1L, "room.jpg", "image/jpeg", 3, new byte[]{1, 2, 3});
        Room requestedRoom = new Room(1L, "Room", "AR", "BA", "Desc",
                RoomType.SHARED, BedType.TWIN, false, true, "Wifi", owner, image, BigDecimal.TEN);
        LocalDate today = LocalDate.now();

        return new Contact(contactId, requestedRoom, LocalDateTime.now(), SwapStatus.PENDING, false,
                BigDecimal.TEN, requester, null, new DateRange(today.minusDays(2), today.minusDays(1)), null);
    }
}
