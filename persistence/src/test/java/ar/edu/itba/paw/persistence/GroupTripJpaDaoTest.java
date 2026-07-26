package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.interfaces.persistence.GroupTripDao;
import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.trip.GroupTrip;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.trip.Trip;
import ar.edu.itba.paw.model.trip.TripStatus;
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
import java.util.Optional;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class GroupTripJpaDaoTest {
    private static final long OWNER_ID = 1L; // Alice Owner from inserts.sql
    private static final long BOB_ID   = 2L; // Bob (dueño del grupo 2)
    private static final String TRIP_NAME = "Test Trip to Asia";
    private static final LocalDate START_DATE = LocalDate.of(2027, 5, 1);
    private static final LocalDate END_DATE = LocalDate.of(2027, 5, 30);

    @Autowired
    private DataSource ds;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private GroupTripDao groupTripDao;

    private JdbcTemplate jdbcTemplate;

    private User owner;
    private DateRange dateRange;

    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(ds);
        // Retrieve the User entity for the owner from the database
        owner = em.find(User.class, OWNER_ID);
        // Create the DateRange object
        dateRange = new DateRange(START_DATE, END_DATE);

        GroupTrip gt1 = em.find(GroupTrip.class, 1L);
        GroupTrip gt2 = em.find(GroupTrip.class, 2L);
        assertNotNull(gt1);
        assertNotNull(gt2);

        gt1.setStatus(TripStatus.UPCOMING);
        gt2.setStatus(TripStatus.DONE);
        em.flush();
    }

    @Rollback
    @Test
    public void testCreate() {
        // Count initial rows for verification
        int initialRows = JdbcTestUtils.countRowsInTable(jdbcTemplate, "group_trip");

        // 1. Exercise: Create a new GroupTrip
        GroupTrip groupTrip = groupTripDao.create(
                owner,
                TRIP_NAME,
                dateRange
        );

        // Ensure the transaction is committed to the database for row counting
        em.flush();

        // 2. Assertion
        assertNotNull(groupTrip);
        assertTrue(groupTrip.getId() > 0);
        assertEquals(owner, groupTrip.getOwner());
        assertEquals(TRIP_NAME, groupTrip.getTitle());
        assertEquals(START_DATE, groupTrip.getDateRange().getStartDate());
        assertEquals(END_DATE, groupTrip.getDateRange().getEndDate());

        // 3. Database verification
        Assert.assertEquals(initialRows + 1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "group_trip"));

        // Verify content correctness in the DB
        Assert.assertEquals(1, JdbcTestUtils.countRowsInTableWhere(
                jdbcTemplate,
                "group_trip",
                String.format("title = '%s' AND id_user = %d AND start_date = '%s'",
                        TRIP_NAME,
                        OWNER_ID,
                        START_DATE.toString())
        ));
    }

    @Rollback
    @Test
    public void testFindGroupTripById_Found() {
        Optional<GroupTrip> maybeGt = groupTripDao.findGroupTripById(1L);

        assertTrue(maybeGt.isPresent());
        GroupTrip gt = maybeGt.get();
        assertEquals(1L, gt.getId());
        assertEquals(OWNER_ID, gt.getOwner().getId());
    }


    @Rollback
    @Test
    public void testFindGroupTripById_NotFound() {
        Optional<GroupTrip> maybeGt = groupTripDao.findGroupTripById(9999L);
        assertFalse(maybeGt.isPresent());
    }

    @Rollback
    @Test
    public void testCountGroupTripsByOwnerId_Found() {
        int countUpcomingAlice = groupTripDao.countGroupTripsByOwnerId(
                OWNER_ID,
                TripStatus.UPCOMING
        );
        assertEquals(1, countUpcomingAlice);

        int countCompletedBob = groupTripDao.countGroupTripsByOwnerId(
                BOB_ID,
                TripStatus.DONE
        );
        assertEquals(1, countCompletedBob);
    }

    @Rollback
    @Test
    public void testCountGroupTripsByOwnerId_NoStatusFilter() {
        int countAlice = groupTripDao.countGroupTripsByOwnerId(OWNER_ID, null);

        assertEquals(1, countAlice);
    }

    @Rollback
    @Test
    public void testFindGroupTripsByOwnerId_NoStatusFilter() {
        List<GroupTrip> groupTrips = groupTripDao.findGroupTripsByOwnerId(OWNER_ID, null, 1, 10);

        assertEquals(1, groupTrips.size());
        assertEquals(OWNER_ID, groupTrips.get(0).getOwner().getId());
        assertEquals("European Tour", groupTrips.get(0).getTitle());
    }

    @Rollback
    @Test
    public void testFindTripsForGroupTripAssociation_FiltersByInternalTripCountry() {
        List<Trip> trips = groupTripDao.findTripsForGroupTripAssociation(
                OWNER_ID,
                TripStatus.UPCOMING,
                "France",
                null,
                null,
                1,
                10
        );

        assertEquals(1, trips.size());
        assertEquals("France", trips.get(0).getCountry());
        assertEquals(1L, trips.get(0).getGroupTrip().getId());
        assertEquals(1, groupTripDao.countTripsForGroupTripAssociation(OWNER_ID, TripStatus.UPCOMING, "France", null, null));
    }

    @Rollback
    @Test
    public void testFindTripsForGroupTripAssociation_MatchesWhenSwapDatesAreStrictlyInsideTripRange() {
        List<Trip> trips = groupTripDao.findTripsForGroupTripAssociation(
                OWNER_ID,
                TripStatus.UPCOMING,
                "France",
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 15),
                1,
                10
        );

        assertEquals(1, trips.size());
        assertEquals(LocalDate.of(2026, 3, 8), trips.get(0).getDateRange().getStartDate());
        assertEquals(LocalDate.of(2026, 3, 16), trips.get(0).getDateRange().getEndDate());
    }

    @Rollback
    @Test
    public void testFindTripsForGroupTripAssociation_MatchesWhenSwapStartsOnTripStart() {
        List<Trip> trips = groupTripDao.findTripsForGroupTripAssociation(
                OWNER_ID,
                TripStatus.UPCOMING,
                "France",
                LocalDate.of(2026, 3, 8),
                LocalDate.of(2026, 3, 12),
                1,
                10
        );

        assertEquals(1, trips.size());
        assertEquals(1, groupTripDao.countTripsForGroupTripAssociation(
                OWNER_ID,
                TripStatus.UPCOMING,
                "France",
                LocalDate.of(2026, 3, 8),
                LocalDate.of(2026, 3, 12)
        ));
    }

    @Rollback
    @Test
    public void testFindTripsForGroupTripAssociation_MatchesWhenSwapEndsOnTripEnd() {
        List<Trip> trips = groupTripDao.findTripsForGroupTripAssociation(
                OWNER_ID,
                TripStatus.UPCOMING,
                "France",
                LocalDate.of(2026, 3, 12),
                LocalDate.of(2026, 3, 16),
                1,
                10
        );

        assertEquals(1, trips.size());
        assertEquals(1, groupTripDao.countTripsForGroupTripAssociation(
                OWNER_ID,
                TripStatus.UPCOMING,
                "France",
                LocalDate.of(2026, 3, 12),
                LocalDate.of(2026, 3, 16)
        ));
    }

    @Rollback
    @Test
    public void testFindTripsForGroupTripAssociation_DoesNotMatchWhenSwapStartsBeforeTripStart() {
        List<Trip> trips = groupTripDao.findTripsForGroupTripAssociation(
                OWNER_ID,
                TripStatus.UPCOMING,
                "France",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 12),
                1,
                10
        );

        assertTrue(trips.isEmpty());
        assertEquals(0, groupTripDao.countTripsForGroupTripAssociation(
                OWNER_ID,
                TripStatus.UPCOMING,
                "France",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 12)
        ));
    }

    @Rollback
    @Test
    public void testFindTripsForGroupTripAssociation_DoesNotMatchWhenSwapEndsAfterTripEnd() {
        List<Trip> trips = groupTripDao.findTripsForGroupTripAssociation(
                OWNER_ID,
                TripStatus.UPCOMING,
                "France",
                LocalDate.of(2026, 3, 12),
                LocalDate.of(2026, 3, 20),
                1,
                10
        );

        assertTrue(trips.isEmpty());
        assertEquals(0, groupTripDao.countTripsForGroupTripAssociation(
                OWNER_ID,
                TripStatus.UPCOMING,
                "France",
                LocalDate.of(2026, 3, 12),
                LocalDate.of(2026, 3, 20)
        ));
    }

    @Rollback
    @Test
    public void testFindTripsForGroupTripAssociation_DoesNotMatchPartialDateOverlap() {
        List<Trip> trips = groupTripDao.findTripsForGroupTripAssociation(
                OWNER_ID,
                TripStatus.UPCOMING,
                "France",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                1,
                10
        );

        assertTrue(trips.isEmpty());
        assertEquals(0, groupTripDao.countTripsForGroupTripAssociation(
                OWNER_ID,
                TripStatus.UPCOMING,
                "France",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)
        ));
    }

    @Rollback
    @Test
    public void testFindTripsForGroupTripAssociation_StatusAppliesToGroupTrip() {
        List<Trip> trips = groupTripDao.findTripsForGroupTripAssociation(
                OWNER_ID,
                TripStatus.DONE,
                "France",
                null,
                null,
                1,
                10
        );

        assertTrue(trips.isEmpty());
    }

    @Rollback
    @Test
    public void testFindTripsForGroupTripAssociation_DoesNotDuplicateGroupTrips() {
        GroupTrip groupTrip = em.find(GroupTrip.class, 1L);
        em.persist(new Trip(groupTrip, "France", new DateRange(
                LocalDate.of(2026, 3, 20),
                LocalDate.of(2026, 3, 25)
        )));
        em.flush();
        em.clear();

        List<Trip> trips = groupTripDao.findTripsForGroupTripAssociation(
                OWNER_ID,
                TripStatus.UPCOMING,
                "France",
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 15),
                1,
                10
        );

        assertEquals(1, trips.size());
        assertEquals(1, groupTripDao.countTripsForGroupTripAssociation(
                OWNER_ID,
                TripStatus.UPCOMING,
                "France",
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 15)
        ));
    }

    @Rollback
    @Test
    public void testUpdateDates() {
        long groupTripId = 1L;
        LocalDate newStart = LocalDate.of(2026, 2, 1);
        LocalDate newEnd   = LocalDate.of(2026, 5, 1);
        DateRange newRange = new DateRange(newStart, newEnd);

        groupTripDao.updateDates(groupTripId, newRange);
        em.flush();
        em.clear();

        GroupTrip updated = em.find(GroupTrip.class, groupTripId);
        assertNotNull(updated);
        assertEquals(newStart, updated.getDateRange().getStartDate());
        assertEquals(newEnd, updated.getDateRange().getEndDate());
    }

    @Rollback
    @Test
    public void testGetEarliestTripStartDate_Found() {
        // Para group_trip 1, en inserts.sql:
        // Trip 1: 2026-03-08..2026-03-16
        Optional<LocalDate> maybeDate = groupTripDao.getEarliestTripStartDate(1L);

        assertTrue(maybeDate.isPresent());
        assertEquals(LocalDate.of(2026, 3, 8), maybeDate.get());
    }

    @Rollback
    @Test
    public void testGetLatestTripEndDate_Found() {
        // Para group_trip 1, único trip: end_date = 2026-03-16
        Optional<LocalDate> maybeDate = groupTripDao.getLatestTripEndDate(1L);

        assertTrue(maybeDate.isPresent());
        assertEquals(LocalDate.of(2026, 3, 16), maybeDate.get());
    }

    @Rollback
    @Test
    public void testUpdateStatus_Found() {
        long groupTripId = 1L;

        boolean updated = groupTripDao.updateStatus(groupTripId, TripStatus.DONE);
        assertTrue(updated);

        em.flush();
        em.clear();

        GroupTrip reloaded = em.find(GroupTrip.class, groupTripId);
        assertNotNull(reloaded);
        assertEquals(TripStatus.DONE, reloaded.getStatus());
    }

    @Rollback
    @Test
    public void testUpdateStatus_NotFound() {
        boolean updated = groupTripDao.updateStatus(9999L, TripStatus.DONE);
        assertFalse(updated);
    }
    
}
