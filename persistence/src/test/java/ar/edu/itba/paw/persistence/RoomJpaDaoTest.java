package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Image;
import ar.edu.itba.paw.model.DTO.RoomSearchCriteria;
import ar.edu.itba.paw.model.rooms.Amenity;
import ar.edu.itba.paw.model.rooms.BedType;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.rooms.RoomType;
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


import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class RoomJpaDaoTest {

    private static final long USER_ID = 1;
    private static final String TITLE = "title";
    private static final String COUNTRY = "Spain";
    private static final String CITY = "Barcelona";
    private static final String DESCRIPTION = "description";
    private static final RoomType ROOM_TYPE = RoomType.HOME;
    private static final BedType BED_TYPE = BedType.KING;
    private static final boolean PRIVATE_BATHROOM = false;
    private static final boolean PRIVATE_KITCHEN = false;
    private static final String AMENITY = "amenities";
    private static final BigDecimal DAY_PRICE = BigDecimal.valueOf(10000);

    private static final long ALICE_ID = 1L;
    private static final long IMAGE_ID = 3L;

    @Autowired
    private DataSource ds;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private RoomJpaDao roomDao;

    private JdbcTemplate jdbcTemplate;

    private User user;
    private Image image;

    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(ds);
        user = em.find(User.class, ALICE_ID);
        image = em.find(Image.class, IMAGE_ID);
    }

    @Rollback
    @Test
    public void testCreate() {

        Room room = roomDao.create(
                user.getId(),
                TITLE,
                COUNTRY,
                CITY,
                DESCRIPTION,
                ROOM_TYPE,
                BED_TYPE,
                PRIVATE_BATHROOM,
                PRIVATE_KITCHEN,
                AMENITY,
                image.getId(), DAY_PRICE
        );

        em.flush();

        assertNotNull(room);
        assertEquals(user, room.getOwner());
        assertEquals(TITLE, room.getTitle());
        assertEquals(COUNTRY, room.getCountry());
        assertEquals(CITY, room.getCity());
        assertEquals(DESCRIPTION, room.getDescription());
        assertEquals(ROOM_TYPE, room.getRoomType());
        assertEquals(BED_TYPE, room.getBedType());
        assertEquals(PRIVATE_BATHROOM, room.isPrivateBathroom());
        assertEquals(PRIVATE_KITCHEN, room.isPrivateKitchen());
        assertTrue(room.getAmenities().contains(AMENITY));
        assertEquals(image.getId(), room.getImageId().longValue());
        Assert.assertEquals(1, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "room", String.format("title = '%s'", TITLE)));
    }

    @Test
    public void searchRoomsFiltersByOneAmenity() {
        RoomSearchCriteria criteria = new RoomSearchCriteria();
        criteria.setAmenities(List.of(Amenity.POOL));

        List<Room> rooms = roomDao.searchRooms(criteria, 1, 10);

        assertRoomIds(rooms, 4L, 7L);
        assertEquals(2, roomDao.countSearchRooms(criteria));
    }

    @Test
    public void searchRoomsRequiresAllSelectedAmenities() {
        RoomSearchCriteria criteria = new RoomSearchCriteria();
        criteria.setAmenities(List.of(Amenity.WIFI, Amenity.POOL));

        List<Room> rooms = roomDao.searchRooms(criteria, 1, 10);

        assertRoomIds(rooms, 4L, 7L);
        assertEquals(2, roomDao.countSearchRooms(criteria));
    }

    @Test
    public void searchRoomsMatchesAmenityWithStoredExtraSpaces() {
        RoomSearchCriteria criteria = new RoomSearchCriteria();
        criteria.setAmenities(List.of(Amenity.AC));

        List<Room> rooms = roomDao.searchRooms(criteria, 1, 10);

        assertRoomIds(rooms, 7L);
        assertEquals(1, roomDao.countSearchRooms(criteria));
    }

    private void assertRoomIds(List<Room> rooms, Long... expectedIds) {
        assertEquals(List.of(expectedIds), rooms.stream().map(Room::getId).toList());
    }
}
