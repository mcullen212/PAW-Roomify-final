package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.interfaces.persistence.TripContactDao; // Assuming TripContactDao interface exists
import ar.edu.itba.paw.model.swaps.Contact;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.trip.Trip;
import ar.edu.itba.paw.model.trip.TripContact;
import ar.edu.itba.paw.persistence.config.TestConfig;
import org.junit.Assert;
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

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class TripContactJpaDaoTest {
    private static final long TRIP_ID = 1L;      // Trip 1 (France)
    private static final long CONTACT_ID = 2L;   // Contact 2 (ACCEPTED Swap)
    private static final long ROOM_ID = 2L;      // Room 2 (Big House BA) - This is the room involved in Contact 2

    @Autowired
    private DataSource ds;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private TripContactDao tripContactDao;

    private JdbcTemplate jdbcTemplate;

    private Trip trip;
    private Contact contact;
    private Room room;

    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(ds);
        trip = em.find(Trip.class, TRIP_ID);
        contact = em.find(Contact.class, CONTACT_ID);
        room = em.find(Room.class, ROOM_ID);
    }

    @Rollback
    @Test
    public void testCreate() {
        int initialRows = JdbcTestUtils.countRowsInTable(jdbcTemplate, "trip_contact");

        // 1. Exercise: Create a new TripContact
        TripContact tripContact = tripContactDao.create(
                trip.getId(),
                contact.getId(),
                room.getId()
        );

        em.flush();

        // 2. Assertions
        assertNotNull(tripContact);
        assertTrue(tripContact.getId() > 0);
        assertEquals(trip, tripContact.getTrip());
        assertEquals(contact, tripContact.getContact());

        // 3. Database verification
        Assert.assertEquals(initialRows + 1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "trip_contact"));

        Assert.assertEquals(1, JdbcTestUtils.countRowsInTableWhere(
                jdbcTemplate,
                "trip_contact",
                String.format("trip_id = %d AND contact_id = %d AND room_id_involved = %d",
                        TRIP_ID,
                        CONTACT_ID,
                        ROOM_ID)
        ));
    }

    @Rollback
    @Test
    public void testFindTripContactById_Found() {
        // In DB: trip_contact id=1 existe
        TripContact tc = tripContactDao.findTripContactById(1L).orElse(null);

        assertNotNull(tc);
        assertEquals(1L, tc.getId());
        assertEquals(1L, tc.getTrip().getId());
        assertEquals(1L, tc.getContact().getId());
    }

    @Rollback
    @Test
    public void testFindTripContactById_NotFound() {
        assertTrue(tripContactDao.findTripContactById(9999L).isEmpty());
    }

    @Rollback
    @Test
    public void testTripContactList_Found() {
        // In DB: trip_contact 1 has trip_id=1
        int page = 1;
        int pageSize = 10;

        var list = tripContactDao.tripContactList(TRIP_ID, page, pageSize);

        assertNotNull(list);
        assertEquals(1, list.size());

        TripContact tc = list.get(0);
        assertEquals(TRIP_ID, tc.getTrip().getId());
    }

    @Rollback
    @Test
    public void testCountByTripId_Found() {
        // trip_id=1 appears exactly once in inserts.sql
        int count = tripContactDao.countByTripId(TRIP_ID);

        assertEquals(1, count);
    }
}