package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.interfaces.persistence.TripDao; // Assuming a TripDao interface exists
import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.trip.GroupTrip;
import ar.edu.itba.paw.model.trip.Trip;
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

import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class TripJpaDaoTest {
    // Using GroupTrip 1 (European Tour, ID=1) from inserts.sql
    private static final long GROUP_TRIP_ID_1 = 1L;
    // GroupTrip 2: South America Getaway (id = 2, owner = Bob id=2)

    private static final String COUNTRY_NEW = "Germany";
    private static final LocalDate START_DATE_NEW = LocalDate.of(2026, 4, 1);
    private static final LocalDate END_DATE_NEW = LocalDate.of(2026, 4, 15);

    private static final String COUNTRY_FRANCE = "France";

    private static final long ALICE_ID = 1L;


    @Autowired
    private DataSource ds;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private TripDao tripDao;

    private JdbcTemplate jdbcTemplate;

    private GroupTrip groupTrip1;

    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(ds);
        // Retrieve the GroupTrip entity which will be the parent of the new Trip
        groupTrip1 = em.find(GroupTrip.class, GROUP_TRIP_ID_1);
    }

    @Rollback
    @Test
    public void testCreate() {
        int initialRows = JdbcTestUtils.countRowsInTable(jdbcTemplate, "trip");

        DateRange dateRange = new DateRange(START_DATE_NEW, END_DATE_NEW);
        Trip trip = tripDao.create(
                GROUP_TRIP_ID_1,
                COUNTRY_NEW,
                dateRange
        );

        em.flush();

        assertNotNull(trip);
        assertTrue(trip.getId() > 0);
        assertEquals(groupTrip1, trip.getGroupTrip());
        assertEquals(COUNTRY_NEW, trip.getCountry());
        assertEquals(START_DATE_NEW, trip.getDateRange().getStartDate());
        assertEquals(END_DATE_NEW, trip.getDateRange().getEndDate());

        Assert.assertEquals(initialRows + 1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "trip"));

        Assert.assertEquals(1, JdbcTestUtils.countRowsInTableWhere(
                jdbcTemplate,
                "trip",
                String.format("country = '%s' AND id_group_trip = %d AND start_date = '%s'",
                        COUNTRY_NEW,
                        GROUP_TRIP_ID_1,
                        START_DATE_NEW)
        ));
    }

    @Rollback
    @Test
    public void testFindTripsByGroupTripId_GroupTrip1() {
        // En inserts.sql, GroupTrip 1 tiene Trip 1 (France, 2026-03-08 a 2026-03-16)
        int page = 1;
        int pageSize = 10;

        List<Trip> trips = tripDao.findTripsByGroupTripId(GROUP_TRIP_ID_1, page, pageSize);

        assertNotNull(trips);
        assertEquals(1, trips.size());

        Trip t = trips.get(0);
        assertEquals(COUNTRY_FRANCE, t.getCountry());
        assertEquals(LocalDate.of(2026, 3, 8), t.getDateRange().getStartDate());
        assertEquals(LocalDate.of(2026, 3, 16), t.getDateRange().getEndDate());
        assertEquals(GROUP_TRIP_ID_1, t.getGroupTrip().getId());
    }

    @Rollback
    @Test
    public void testFindTripsByGroupTripId_NoResults() {
        int page = 1;
        int pageSize = 10;

        List<Trip> trips = tripDao.findTripsByGroupTripId(9999L, page, pageSize);

        assertNotNull(trips);
        assertTrue(trips.isEmpty());
    }

    @Rollback
    @Test
    public void testCountTripsByGroupTripId_GroupTrip1() {
        // inserts.sql: GroupTrip 1 tiene exactamente 1 Trip
        int count = tripDao.countTripsByGroupTripId(GROUP_TRIP_ID_1);
        assertEquals(1, count);
    }

    @Rollback
    @Test
    public void testExistsOverlappingTrip_ReturnsTrueWhenDatesOverlap() {
        DateRange overlappingRange = new DateRange(
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 20)
        );

        assertTrue(tripDao.existsOverlappingTrip(GROUP_TRIP_ID_1, overlappingRange));
    }

    @Rollback
    @Test
    public void testExistsOverlappingTrip_ReturnsFalseWhenDatesDoNotOverlap() {
        DateRange nonOverlappingRange = new DateRange(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 15)
        );

        assertFalse(tripDao.existsOverlappingTrip(GROUP_TRIP_ID_1, nonOverlappingRange));
    }

    @Rollback
    @Test
    public void testExistsOverlappingTrip_ReturnsFalseWhenDatesTouchBoundary() {
        DateRange boundaryTouchingRange = new DateRange(
                LocalDate.of(2026, 3, 16),
                LocalDate.of(2026, 3, 20)
        );

        assertFalse(tripDao.existsOverlappingTrip(GROUP_TRIP_ID_1, boundaryTouchingRange));
    }

    @Rollback
    @Test
    public void testBringMyTrips_FoundForAliceInFrance() {
        User alice = em.find(User.class, ALICE_ID);
        assertNotNull(alice);

        // Alice tiene GroupTrip 1 (European Tour) y Trip 1 en France del 8 al 16 de marzo 2026.
        LocalDate searchStart = LocalDate.of(2026, 3, 1);
        LocalDate searchEnd   = LocalDate.of(2026, 3, 31);

        int page = 1;
        int pageSize = 10;

        // Solo se llama a bringMytrips
        List<Trip> trips = tripDao.bringMytrips(
                COUNTRY_FRANCE,
                searchStart,
                searchEnd,
                alice,
                page,
                pageSize
        );

        assertNotNull(trips);
        assertEquals(1, trips.size());

        Trip t = trips.get(0);
        assertEquals(COUNTRY_FRANCE, t.getCountry());
        assertEquals(LocalDate.of(2026, 3, 8), t.getDateRange().getStartDate());
        assertEquals(LocalDate.of(2026, 3, 16), t.getDateRange().getEndDate());
        assertEquals(alice.getId(), t.getGroupTrip().getOwner().getId());
    }




}
