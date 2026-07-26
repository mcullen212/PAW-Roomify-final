package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.swaps.Contact;
import ar.edu.itba.paw.model.DTO.RoomReviewStats;
import ar.edu.itba.paw.model.rooms.Review;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.User;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class ReviewJpaDaoTest {
    private static final long ALICE_ID = 1L;
    private static final long BOB_ID = 2L;
    private static final long CHARLIE_ID = 3L;

    private static final long ROOM_ID_1 = 1L; // Alice's Room
    private static final long ROOM_ID_2 = 2L; // Bob's Room

    private static final long CONTACT_ID_1 = 1L; // Contact related to Room 1 reviews
    private static final long CONTACT_ID_2 = 2L;
    private static final long TOTAL_INITIAL_REVIEWS = 3L;

    private static final String TABLE_REVIEW = "REVIEW";

    @Autowired
    private DataSource ds;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ReviewJpaDao reviewDao;

    private JdbcTemplate jdbcTemplate;

    // Entities loaded from inserts.sql
    private User alice;
    private User bob;
    private User charlie;
    private Room room1;
    private Room room2;
    private Contact contact1;
    private Contact contact2;


    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(ds);

        // 1. Load USERS from inserts.sql
        alice = em.find(User.class, ALICE_ID);
        bob = em.find(User.class, BOB_ID);
        charlie = em.find(User.class, CHARLIE_ID);

        // 2. Load ROOMS from inserts.sql
        room1 = em.find(Room.class, ROOM_ID_1);
        room2 = em.find(Room.class, ROOM_ID_2);

        // 3. Load CONTACTS from inserts.sql
        contact1 = em.find(Contact.class, CONTACT_ID_1);
        contact2 = em.find(Contact.class, CONTACT_ID_2);
    }

    @Rollback
    @Test
    public void testSave() {
        // 1. Setup: Alice (Reviewer) reviews a transaction
        final double rating = 4.5;
        final String comment = "New review for testing save!";
        final LocalDateTime createdAt = LocalDateTime.now().withNano(0);

        // Check initial review count
        assertEquals(TOTAL_INITIAL_REVIEWS, JdbcTestUtils.countRowsInTable(jdbcTemplate, TABLE_REVIEW));

        // 2. Exercise: Alice reviews contact 1
        final Review review = reviewDao.save(contact1, alice, rating, comment, createdAt);

        // Ensure persistence happens
        em.flush();

        // 3. Postcondiciones (Assert)
        assertNotNull(review);
        assertEquals(contact1.getId(), review.getContactId());
        assertEquals(alice.getId(), review.getReviewer().getId());
        assertEquals(rating, review.getRating(), 0.001);

        // Check if exactly one new row was added (Total initial + 1)
        assertEquals(TOTAL_INITIAL_REVIEWS + 1, JdbcTestUtils.countRowsInTable(jdbcTemplate, TABLE_REVIEW));
    }


    @Rollback
    @Test
    public void testRoomRating_Room1() {
        // 1. Setup: Reviews for Room 1 already exist in inserts.sql: 5.0 (Alice) and 3.0 (Bob)
        // Average: (5.0 + 3.0) / 2 = 4.0

        // 2. Exercise (Rating for Room 1)
        final double rating = reviewDao.roomRating(ROOM_ID_1);

        // 3. Postcondiciones (Assert)
        assertEquals(4.0, rating, 0.0001);
    }

    @Rollback
    @Test
    public void testRoomRating_Room2() {
        // 1. Setup: Reviews for Room 2 already exist in inserts.sql: 4.0 (Charlie)
        // Average: 4.0

        // 2. Exercise (Rating for Room 2)
        final double rating = reviewDao.roomRating(ROOM_ID_2);

        // 3. Postcondiciones (Assert)
        assertEquals(4.0, rating, 0.0001);
    }

    @Rollback
    @Test
    public void testGetReviewStatsByRoomIds_WithResults() {
        Map<Long, RoomReviewStats> stats = reviewDao.getReviewStatsByRoomIds(List.of(ROOM_ID_1, ROOM_ID_2));

        assertEquals(2, stats.size());
        assertTrue(stats.containsKey(ROOM_ID_1));
        assertEquals(2, stats.get(ROOM_ID_1).getTotalReviews());
        assertEquals(4.0, stats.get(ROOM_ID_1).getAverageRating(), 0.0001);
        assertTrue(stats.containsKey(ROOM_ID_2));
        assertEquals(1, stats.get(ROOM_ID_2).getTotalReviews());
        assertEquals(4.0, stats.get(ROOM_ID_2).getAverageRating(), 0.0001);
    }

    @Rollback
    @Test
    public void testGetReviewStatsByRoomIds_WithoutReviews() {
        Map<Long, RoomReviewStats> stats = reviewDao.getReviewStatsByRoomIds(List.of(9999L));

        assertTrue(stats.isEmpty());
    }


    @Rollback
    @Test
    public void testFindByRoomId_Room1_WithResults() {
        // 1. Setup: Two reviews for Room 1 exist: ID 1 (5.0, older) and ID 2 (3.0, newer)

        // 2. Exercise: Find reviews for Room 1 (should return most recent first)
        final List<Review> results = reviewDao.findByRoomId(ROOM_ID_2, 1, 2);

        // 3. Postcondiciones (Assert)
        assertEquals(1, results.size());

        assertEquals(4.0, results.get(0).getRating(), 0.001);
        assertEquals(charlie.getId(), results.get(0).getReviewer().getId());
    }
}
