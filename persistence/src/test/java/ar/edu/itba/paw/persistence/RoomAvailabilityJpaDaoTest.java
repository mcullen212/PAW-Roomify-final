package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.rooms.RoomAvailability;
import ar.edu.itba.paw.persistence.config.TestConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.matchers.Null;
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
public class RoomAvailabilityJpaDaoTest {
    private static final long ROOM_ID_ALICE = 1L;
    private static final long AVAILABILITY_ID_ALICE = 1L; // March 1 - March 31, 2026
    private static final LocalDate START_DATE_ALICE = LocalDate.of(2026, 3, 1);
    private static final LocalDate END_DATE_ALICE = LocalDate.of(2026, 3, 31);

    private static final LocalDate NEW_START_DATE = LocalDate.of(2027, 1, 1);
    private static final LocalDate NEW_END_DATE = LocalDate.of(2027, 1, 15);

    @Autowired
    private DataSource ds;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private RoomAvailabilityJpaDao roomAvailabilityDao;

    private JdbcTemplate jdbcTemplate;

    // Entities loaded from inserts.sql
    private Room roomAlice;
    private RoomAvailability availabilityAlice;


    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(ds);

        // Load Alice's Room (Room ID 1)
        roomAlice = em.find(Room.class, ROOM_ID_ALICE);
        availabilityAlice = em.find(RoomAvailability.class, AVAILABILITY_ID_ALICE);
    }


    @Rollback
    @Test
    public void testCreate() {
        final int initialRows = JdbcTestUtils.countRowsInTable(jdbcTemplate, "room_availability");

        DateRange range = new DateRange(NEW_START_DATE, NEW_END_DATE);

        // 2. Exercise
        final RoomAvailability newAvailability = roomAvailabilityDao.create(roomAlice.getId(), range);

        // Ensure persistence happens
        em.flush();

        // 3. Postcondiciones (Assert)
        assertNotNull(newAvailability);
        assertEquals(roomAlice.getId(), newAvailability.getRoomId());
        assertEquals(NEW_START_DATE, newAvailability.getRange().getStartDate());
        assertEquals(NEW_END_DATE, newAvailability.getRange().getEndDate());
        assertEquals(initialRows + 1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "room_availability"));
    }

    @Rollback
    @Test
    public void testFindByRoom_WithPreloadedAvailability() {
        // Room 1 (Alice's) should have 1 pre-loaded availability

        // 2. Exercise
        final List<RoomAvailability> result = roomAvailabilityDao.findByRoom(ROOM_ID_ALICE);

        // 3. Postcondiciones (Assert)
        assertEquals(1, result.size());
        final RoomAvailability found = result.get(0);
        assertEquals(ROOM_ID_ALICE, found.getRoomId().longValue());
        assertEquals(START_DATE_ALICE, found.getRange().getStartDate());
        assertEquals(END_DATE_ALICE, found.getRange().getEndDate());
    }

    @Rollback
    @Test
    public void testFindByRoom_NoAvailabilities() {
        // Room 2 (Bob's) has availability ID 2, let's look for Charlie's room (ID 3) which has none.
        final long ROOM_ID_NO_AVAILABILITY = 3L;

        // Exercise
        final List<RoomAvailability> result = roomAvailabilityDao.findByRoom(ROOM_ID_NO_AVAILABILITY);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Rollback
    @Test
    public void testFindByRoomBetween_OverlappingRange_ReturnsAvailability() {
        final List<RoomAvailability> result = roomAvailabilityDao.findByRoomBetween(
                ROOM_ID_ALICE,
                LocalDate.of(2026, 3, 15),
                LocalDate.of(2026, 4, 15)
        );

        assertEquals(1, result.size());
        assertEquals(AVAILABILITY_ID_ALICE, result.get(0).getId().longValue());
    }

    @Rollback
    @Test
    public void testFindByRoomBetween_NonOverlappingRange_ReturnsEmptyList() {
        final List<RoomAvailability> result = roomAvailabilityDao.findByRoomBetween(
                ROOM_ID_ALICE,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 15)
        );

        assertTrue(result.isEmpty());
    }

    @Rollback
    @Test
    public void testFindAvailableIdByRoom_WithAvailability() {
        // Room 1 (Alice's) should have available ID 1

        // Exercise
        final Long maybeId = roomAvailabilityDao.findAvailableIdByRoom(ROOM_ID_ALICE);

        // Assert
        assertEquals(AVAILABILITY_ID_ALICE, maybeId.longValue());
    }

    @Rollback
    @Test
    public void testFindAvailableIdByRoom_NoAvailabilities() {
        // Room ID 3 (Charlie's room, non-existent or no availability)
        final long ROOM_ID_NO_AVAILABILITY = 3L;

        // Exercise
        final Long id = roomAvailabilityDao.findAvailableIdByRoom(ROOM_ID_NO_AVAILABILITY);

        // Assert
        assertNull(id);
    }

}
