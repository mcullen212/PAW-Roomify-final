package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.interfaces.persistence.ContactDao;
import ar.edu.itba.paw.model.swaps.Contact;
import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.swaps.SwapStatus;
import ar.edu.itba.paw.persistence.config.TestConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class ContactJpaDaoTest {
    private static final long ALICE_ID = 1L; // Owner of Room 1 (Paris Studio)
    private static final long BOB_ID = 2L;   // Owner of Room 2 (BA House)
    private static final long CHARLIE_ID = 3L; // Swapper in Contact 2

    private static final long ROOM_ID_1 = 1L; // Paris Studio (Alice's)
    private static final long ROOM_ID_2 = 2L; // BA House (Bob's)

    private static final long CONTACT_ID_SWAP = 2L; // Contact 2: ACCEPTED Swap
    private static final long CONTACT_ID_MONEY = 1L; // Contact 1: PENDING Money Offer

    private static final String TABLE_CONTACT = "contact";

    @Autowired
    private DataSource ds;

    @Autowired
    private ContactDao contactDao;

    @PersistenceContext
    private EntityManager em;

    private JdbcTemplate jdbcTemplate;

    // Entities loaded from inserts.sql
    private User alice;
    private User bob;
    private Room room1;
    private Room room2;
    private Contact contactSwap; // Contact 2 (ACCEPTED)
    private Contact contactMoney; // Contact 1 (PENDING)


    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(ds);

        // Load necessary entities from the pre-loaded database state
        alice = em.find(User.class, ALICE_ID);
        bob = em.find(User.class, BOB_ID);
        room1 = em.find(Room.class, ROOM_ID_1);
        room2 = em.find(Room.class, ROOM_ID_2);
        contactSwap = em.find(Contact.class, CONTACT_ID_SWAP);
        contactMoney = em.find(Contact.class, CONTACT_ID_MONEY); // Load Contact 1

        // Safety checks to prevent NullPointerExceptions later
        assertNotNull("Alice user must be loaded.", alice);
        assertNotNull("Bob user must be loaded.", bob);
        assertNotNull("Room 1 must be loaded.", room1);
        assertNotNull("Contact 2 (Swap) must be loaded.", contactSwap);
        assertNotNull("Contact 1 (Money) must be loaded.", contactMoney);
    }

    @Rollback
    @Test
    public void testCreateContactRoomSwap() {
        int initialRows = JdbcTestUtils.countRowsInTable(jdbcTemplate, TABLE_CONTACT);
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 10, 0);
        DateRange requestedRange = new DateRange(LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 13));

        // Alice (owner of Room 1) offers Room 1 to Bob (owner of Room 2)
        // Alice is the offerer, Room 2 is the requested room, Room 1 is the offered room.
        Contact contact = contactDao.create(alice.getId(), room2, room1, now,
                SwapStatus.PENDING, true, null, requestedRange, null);

        em.flush();
        long contactId = contact.getId();
        em.clear();

        Contact persisted = em.find(Contact.class, contactId);
        assertNotNull(persisted);
        assertEquals(room2.getId(), persisted.getRoomRequested().getId());
        assertEquals(room1.getId(), persisted.getRoomOffered().getId());
        assertEquals(requestedRange.getStartDate(), persisted.getRequestedRange().getStartDate());
        assertEquals(requestedRange.getEndDate(), persisted.getRequestedRange().getEndDate());
        assertTrue(persisted.isSwap());
        assertEquals(SwapStatus.PENDING, persisted.getStatus());

        assertEquals(initialRows + 1, JdbcTestUtils.countRowsInTable(jdbcTemplate, TABLE_CONTACT));
    }

    @Rollback
    @Test
    public void testCreateContactMoneySwap() {
        int initialRows = JdbcTestUtils.countRowsInTable(jdbcTemplate, TABLE_CONTACT);
        LocalDateTime now = LocalDateTime.of(2026, 7, 1, 10, 0);
        DateRange requestedRange = new DateRange(LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 13));
        BigDecimal moneyOffer = BigDecimal.valueOf(100.00).setScale(2);

        // Bob requests Alice's room (Room 1) and offers money.
        // Bob is the offerUser. Room 1 is the requestedRoom.
        Contact contact = contactDao.create(bob.getId(), room1, null, now,
                SwapStatus.PENDING, false, moneyOffer, requestedRange, null);

        em.flush();
        long contactId = contact.getId();
        em.clear();

        Contact persisted = em.find(Contact.class, contactId);
        assertNotNull(persisted);
        assertEquals(room1.getId(), persisted.getRoomRequested().getId());
        assertEquals(moneyOffer, persisted.getMoneyOffer());
        assertEquals(requestedRange.getStartDate(), persisted.getRequestedRange().getStartDate());
        assertEquals(requestedRange.getEndDate(), persisted.getRequestedRange().getEndDate());
        assertFalse(persisted.isSwap());
        assertEquals(SwapStatus.PENDING, persisted.getStatus());

        assertEquals(initialRows + 1, JdbcTestUtils.countRowsInTable(jdbcTemplate, TABLE_CONTACT));
    }

    @Rollback
    @Test
    public void testFindPendingContactsForRoomOnlyReturnsPendingOnRequestedOrOfferedSide() {
        Contact offeredSidePending = contactDao.create(alice.getId(), room2, room1, LocalDateTime.now(),
                SwapStatus.PENDING, true, null, new DateRange(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 12)), null);
        em.flush();

        List<Contact> contacts = contactDao.findPendingContactsForRoom(ROOM_ID_1);

        assertEquals(2, contacts.size());
        assertTrue(contacts.stream().anyMatch(c -> c.getId() == CONTACT_ID_MONEY));
        assertTrue(contacts.stream().anyMatch(c -> c.getId() == offeredSidePending.getId()));
        assertFalse(contacts.stream().anyMatch(c -> c.getId() == CONTACT_ID_SWAP));
        assertTrue(contacts.stream().allMatch(c -> c.getStatus() == SwapStatus.PENDING));
    }

    @Rollback
    @Test
    public void testFindPendingRequestedOverlapReturnsPendingContactsWithTheirOwnRange() {
        DateRange acceptedRange = new DateRange(LocalDate.of(2026, 3, 12), LocalDate.of(2026, 3, 14));
        Contact acceptedOverlap = new Contact(room1, LocalDateTime.now(), SwapStatus.ACCEPTED,
                false, BigDecimal.valueOf(800.00).setScale(2), bob, null, acceptedRange, null);
        em.persist(acceptedOverlap);
        em.flush();
        em.clear();

        List<Contact> overlappingPending = contactDao.findPendingRequestedOverlap(acceptedOverlap.getId());

        assertEquals(1, overlappingPending.size());
        Contact deletedCandidate = overlappingPending.getFirst();
        assertEquals(CONTACT_ID_MONEY, deletedCandidate.getId());
        assertEquals(LocalDate.of(2026, 3, 10), deletedCandidate.getRequestedRange().getStartDate());
        assertEquals(LocalDate.of(2026, 3, 15), deletedCandidate.getRequestedRange().getEndDate());
        assertEquals(SwapStatus.PENDING, deletedCandidate.getStatus());
    }

    @Rollback
    @Test
    public void testDeletePendingContactsForRoomDeletesOnlyPendingAndTripContacts() {
        assertEquals(1, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "trip_contact", "contact_id = 1"));

        contactDao.deletePendingContactsForRoom(ROOM_ID_1);
        em.flush();

        assertEquals(0, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, TABLE_CONTACT, "id = 1"));
        assertEquals(1, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, TABLE_CONTACT, "id = 2"));
        assertEquals(1, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, TABLE_CONTACT, "id = 3"));
        assertEquals(0, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "trip_contact", "contact_id = 1"));
        assertEquals(1, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "trip_contact", "contact_id = 2"));
    }

    @Rollback
    @Test
    public void testFindExpiredPendingSwapsReturnsOnlyPendingWithRequestedStartBeforeToday() {
        LocalDate today = LocalDate.now();

        Contact expiredPending = contactDao.create(bob.getId(), room1, null, LocalDateTime.now(),
                SwapStatus.PENDING, false, BigDecimal.valueOf(120.00).setScale(2),
                new DateRange(today.minusDays(1), today.plusDays(2)), null);
        Contact startsTodayPending = contactDao.create(bob.getId(), room1, null, LocalDateTime.now(),
                SwapStatus.PENDING, false, BigDecimal.valueOf(130.00).setScale(2),
                new DateRange(today, today.plusDays(2)), null);
        Contact acceptedPastStart = contactDao.create(bob.getId(), room1, null, LocalDateTime.now(),
                SwapStatus.ACCEPTED, false, BigDecimal.valueOf(140.00).setScale(2),
                new DateRange(today.minusDays(2), today.plusDays(1)), null);

        em.flush();
        em.clear();

        List<Contact> expiredContacts = contactDao.findExpiredPendingSwaps();

        assertTrue(expiredContacts.stream().anyMatch(c -> c.getId() == expiredPending.getId()));
        assertFalse(expiredContacts.stream().anyMatch(c -> c.getId() == startsTodayPending.getId()));
        assertFalse(expiredContacts.stream().anyMatch(c -> c.getId() == acceptedPastStart.getId()));
        assertTrue(expiredContacts.stream().allMatch(c -> c.getStatus() == SwapStatus.PENDING));
    }

    @Rollback
    @Test
    public void testConfirmedDateRangeUpdatesStatus() {
        // Use Contact 1 (Money Offer, currently PENDING) to test confirmation process.

        // Initial status check: PENDING
        assertEquals(SwapStatus.PENDING, contactMoney.getStatus());

        // New offered range (The range Bob offers to give Alice if she accepts)
        DateRange newOfferedRange = new DateRange(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 5));

        // Exercise: Confirm the date range (This action should trigger the status change to ACCEPTED if the DAO is designed this way)
        Contact updated = contactDao.confirmedDateRange(contactMoney.getId(), newOfferedRange);

        assertNotNull(updated);
        em.flush();
        em.clear();

        Contact persisted = em.find(Contact.class, CONTACT_ID_MONEY);

        // Assert: The status must change to ACCEPTED upon confirmation, and the offered range must be populated.
        assertNotNull(persisted);
        assertEquals(SwapStatus.ACCEPTED, persisted.getStatus()); // Assume PENDING -> ACCEPTED is part of confirmedDateRange logic

        // Verify the offered range was updated
        assertEquals(newOfferedRange.getStartDate(), persisted.getOfferedRange().getStartDate());
        assertEquals(newOfferedRange.getEndDate(), persisted.getOfferedRange().getEndDate());

        // Verify the database row count hasn't changed
        assertEquals(3, JdbcTestUtils.countRowsInTable(jdbcTemplate, TABLE_CONTACT));
    }
}
