package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.interfaces.persistence.RoomDao;
import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.DTO.RoomSearchCriteria;
import ar.edu.itba.paw.model.rooms.BedType;
import ar.edu.itba.paw.model.rooms.Amenity;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.rooms.RoomType;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;


@Repository
public class RoomJpaDao implements RoomDao {

    @PersistenceContext
    private EntityManager em;
    @Override
    public Optional<Room> findRoomById(long id) {
        return Optional.ofNullable(em.find(Room.class, id)); //PODES ACCEDER A RELACIONES SOLAMENTE DENTRO DEl TRANSACTIONAL
    }
    @Override
    public Room create(long ownerId, String title, String country, String city, String description,
                       RoomType roomType, BedType bedType, boolean privateBathroom, boolean privateKitchen,
                       String attrsJson, long imageId, BigDecimal dayPrice){

        User owner = em.find(User.class, ownerId);
        Image image = em.find(Image.class, imageId);

        Room room = new Room(title, country, city, description,
                roomType, bedType, privateBathroom, privateKitchen, attrsJson, owner, image, dayPrice);
        em.persist(room);
        return room;

    }

    // Pagination
    @Override
    public List<Room> findRoomsByOwnerIdPaging(long ownerId, int page, int pageSize) {
        Query idQuery = em.createNativeQuery("""
                        SELECT r.id
                        FROM room r
                        WHERE r.owner_id = :ownerId
                        ORDER BY r.id
                        LIMIT :limit OFFSET :offset
                        """)
                .setParameter("ownerId", ownerId)
                .setParameter("limit", pageSize)
                .setParameter("offset", (page - 1) * pageSize);
        List<Long> roomIds = toLongIds(idQuery.getResultList());
        return findRoomsByIds(roomIds);
    }

    @Override
    public List<Room> findRoomsByOwnerIdPagingForSwap(long ownerId, int page, int pageSize){
        LocalDate today = LocalDate.now();

        Query idQuery = em.createNativeQuery("""
                        SELECT DISTINCT r.id
                        FROM room r
                        JOIN room_availability ra ON ra.room_id = r.id
                        WHERE r.owner_id = :ownerId
                          AND ra.end_date >= :today
                        ORDER BY r.id
                        LIMIT :limit OFFSET :offset
                        """)
                .setParameter("ownerId", ownerId)
                .setParameter("today", today)
                .setParameter("limit", pageSize)
                .setParameter("offset", (page - 1) * pageSize);
        return findRoomsByIds(toLongIds(idQuery.getResultList()));
    }

    // Pagination
    @Override
    public List<Room> getRoomsPaginated(int page, int pageSize) {
        LocalDate today = LocalDate.now();
        Query idQuery = em.createNativeQuery("""
                        SELECT DISTINCT r.id
                        FROM room r
                        JOIN room_availability ra ON ra.room_id = r.id
                        WHERE ra.end_date >= :today
                        ORDER BY r.id
                        LIMIT :limit OFFSET :offset
                        """)
                .setParameter("today", today)
                .setParameter("limit", pageSize)
                .setParameter("offset", (page - 1) * pageSize);
        List<Long> roomIds = toLongIds(idQuery.getResultList());
        return findRoomsByIds(roomIds);
    }

    public int countRooms(){
        LocalDate today = LocalDate.now();
        Long count = em.createQuery(
                        "SELECT COUNT(DISTINCT r.id) " +
                                "FROM RoomAvailability ra " +
                                "JOIN ra.room r " +
                                "WHERE ra.range.endDate >= :today", Long.class)
                .setParameter("today", today)
                .getSingleResult();
        return count.intValue();
    }

    public List<Room> findByRequesterIdRoom(long userId){
        Query idQuery = em.createNativeQuery("""
                        SELECT c.requested_room_id
                        FROM contact c
                        WHERE c.offer_user_id = :userId
                          AND c.status = 'PENDING'
                        ORDER BY c.contact_date DESC, c.id DESC
                        """)
                .setParameter("userId", userId);
        List<Long> roomIds = toLongIds(idQuery.getResultList());

        if (roomIds.isEmpty()) return Collections.emptyList();

        List<Room> rooms = findRoomsByIds(roomIds);
        Map<Long, Integer> position = new HashMap<>();
        for (int i = 0; i < roomIds.size(); i++) position.putIfAbsent(roomIds.get(i), i);
        rooms.sort(Comparator.comparingInt(room -> position.get(room.getId())));
        return rooms;
    }



    public boolean updateRoom(long roomId, String title, String description, String amenities, BigDecimal dayPrice){
        Room room = em.find(Room.class, roomId);
        if (room == null) {
            return false;
        }
        room.setTitle(title);
        room.setDescription(description);
        room.setAmenities(amenities);
        room.setDayPrice(dayPrice);
        return true;
    }

    @Override
    public int countRoomsByOwnerId(long ownerId){
        Long count = em.createQuery("SELECT COUNT(r) FROM Room r WHERE r.owner.id = :ownerId", Long.class)
                .setParameter("ownerId", ownerId)
                .getSingleResult();
        return count.intValue();    
    }

    @Override
    public int countRoomsByOwnerIdForSwap(long ownerId){
        LocalDate today = LocalDate.now();
        Long count = em.createQuery("SELECT COUNT(DISTINCT r) FROM RoomAvailability ra JOIN ra.room r WHERE r.owner.id = :ownerId AND ra.range.endDate >= :today", Long.class)
        .setParameter("ownerId", ownerId).setParameter("today", today).getSingleResult();
        return count.intValue();
    }

    @Override
    public boolean deleteRoom(long roomId){
        int deleted = em.createQuery("DELETE FROM Room r WHERE r.id = :roomId")
                .setParameter("roomId", roomId)
                .executeUpdate();
        return deleted > 0;
    }

    @Override
    public boolean existsByImageId(long imageId) {
        Long count = em.createQuery("SELECT COUNT(r) FROM Room r WHERE r.image.id = :imageId", Long.class)
                .setParameter("imageId", imageId)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public int countSearchRooms(RoomSearchCriteria c) {
        String pattern = (c.getDestination() == null || c.getDestination().trim().isEmpty())
                ? "%" : "%" + c.getDestination().toLowerCase().trim() + "%";

        StringBuilder sql = new StringBuilder("""
        SELECT COUNT(DISTINCT r.id)
        FROM room r
        JOIN room_availability ra ON ra.room_id = r.id
        WHERE 1=1
        """);

        // Filtros de fechas
        if (c.getCheckIn() != null && c.getCheckOut() != null) {
            sql.append("""
              AND ra.start_date <= :checkIn
              AND ra.end_date >= :checkOut
              AND NOT EXISTS (
                  SELECT 1 FROM contact con
                  WHERE con.status = 'ACCEPTED'
                    AND (
                        (con.requested_room_id = r.id
                         AND con.requested_start_date <= :checkOut
                         AND :checkIn <= con.requested_end_date)
                        OR
                        (con.room_offer_id = r.id
                         AND con.offered_start_date IS NOT NULL
                         AND con.offered_end_date IS NOT NULL
                         AND con.offered_start_date <= :checkOut
                         AND :checkIn <= con.offered_end_date)
                    )
              )
            """);
        }else {
            // Sin fechas: solo rooms cuya disponibilidad no haya terminado
            sql.append("""
          AND ra.end_date >= CURRENT_DATE
        """);
        }

        // Filtros de destino
        sql.append("""
          AND (
              LOWER(COALESCE(r.city, '')) LIKE :pattern OR
              LOWER(COALESCE(r.country, '')) LIKE :pattern OR
              LOWER(COALESCE(r.title, '')) LIKE :pattern
          )
        """);

        // Filtros adicionales
        if (c.getRoomType() != null) {
            sql.append(" AND r.room_type = :roomType");
        }
        if (c.getBedType() != null) {
            sql.append(" AND r.bed_type = :bedType");
        }
        if (c.getPrivateBathroom() != null) {
            sql.append(" AND r.private_bathroom = :privateBathroom");
        }
        if (c.getPrivateKitchen() != null) {
            sql.append(" AND r.private_kitchen = :privateKitchen");
        }
        if (c.getOwnerId() != null) {
            sql.append(" AND r.owner_id = :ownerId");
        }
        appendAmenitiesFilter(sql, c);

        Query query = em.createNativeQuery(sql.toString());
        query.setParameter("pattern", pattern);

        if (c.getCheckIn() != null && c.getCheckOut() != null) {
            query.setParameter("checkIn", c.getCheckIn());
            query.setParameter("checkOut", c.getCheckOut());
        }
        if (c.getRoomType() != null) query.setParameter("roomType", c.getRoomType().name());
        if (c.getBedType() != null) query.setParameter("bedType", c.getBedType().name());
        if (c.getPrivateBathroom() != null) query.setParameter("privateBathroom", c.getPrivateBathroom());
        if (c.getPrivateKitchen() != null) query.setParameter("privateKitchen", c.getPrivateKitchen());
        if (c.getOwnerId() != null) query.setParameter("ownerId", c.getOwnerId());
        setAmenitiesParameters(query, c);

        return ((Number) query.getSingleResult()).intValue();
    }

    @Override
    public List<Room> searchRooms(RoomSearchCriteria c, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        String pattern = (c.getDestination() == null || c.getDestination().trim().isEmpty())
                ? "%" : "%" + c.getDestination().toLowerCase().trim() + "%";

        StringBuilder sql = new StringBuilder("""
        SELECT DISTINCT r.id
        FROM room r
        JOIN room_availability ra ON ra.room_id = r.id
        WHERE 1=1
        """);

        // Fechas
        if (c.getCheckIn() != null && c.getCheckOut() != null) {
            sql.append("""
              AND ra.start_date <= :checkIn
              AND ra.end_date >= :checkOut
              AND NOT EXISTS (
                  SELECT 1 FROM contact con
                  WHERE con.status = 'ACCEPTED'
                    AND (
                        (con.requested_room_id = r.id
                         AND con.requested_start_date <= :checkOut
                         AND :checkIn <= con.requested_end_date)
                        OR
                        (con.room_offer_id = r.id
                         AND con.offered_start_date IS NOT NULL
                         AND con.offered_end_date IS NOT NULL
                         AND con.offered_start_date <= :checkOut
                         AND :checkIn <= con.offered_end_date)
                    )
              )
            """);
        }else {
            sql.append("""
          AND ra.end_date > CURRENT_DATE
        """);
        }

        // Destino
        sql.append("""
          AND (
              LOWER(COALESCE(r.city, '')) LIKE :pattern OR
              LOWER(COALESCE(r.country, '')) LIKE :pattern OR
              LOWER(COALESCE(r.title, '')) LIKE :pattern
          )
        """);

        // Filtros
        if (c.getRoomType() != null) sql.append(" AND r.room_type = :roomType");
        if (c.getBedType() != null) sql.append(" AND r.bed_type = :bedType");
        if (c.getPrivateBathroom() != null) sql.append(" AND r.private_bathroom = :privateBathroom");
        if (c.getPrivateKitchen() != null) sql.append(" AND r.private_kitchen = :privateKitchen");
        if (c.getOwnerId() != null) sql.append(" AND r.owner_id = :ownerId");
        appendAmenitiesFilter(sql, c);

        sql.append(" ORDER BY r.id LIMIT :limit OFFSET :offset");

        Query idQuery = em.createNativeQuery(sql.toString());
        idQuery.setParameter("pattern", pattern);
        idQuery.setParameter("limit", pageSize);
        idQuery.setParameter("offset", offset);

        if (c.getCheckIn() != null && c.getCheckOut() != null) {
            idQuery.setParameter("checkIn", c.getCheckIn());
            idQuery.setParameter("checkOut", c.getCheckOut());
        }
        if (c.getRoomType() != null) idQuery.setParameter("roomType", c.getRoomType().name());
        if (c.getBedType() != null) idQuery.setParameter("bedType", c.getBedType().name());
        if (c.getPrivateBathroom() != null) idQuery.setParameter("privateBathroom", c.getPrivateBathroom());
        if (c.getPrivateKitchen() != null) idQuery.setParameter("privateKitchen", c.getPrivateKitchen());
        if (c.getOwnerId() != null) idQuery.setParameter("ownerId", c.getOwnerId());
        setAmenitiesParameters(idQuery, c);

        List<Long> roomIds = toLongIds(idQuery.getResultList());

        if (roomIds.isEmpty()) return Collections.emptyList();

        return findRoomsByIds(roomIds);
    }

    private List<Room> findRoomsByIds(List<Long> roomIds) {
        if (roomIds.isEmpty()) return Collections.emptyList();

        return em.createQuery(
                        "SELECT r FROM Room r " +
                                "JOIN FETCH r.owner " +
                                "WHERE r.id IN :ids ORDER BY r.id",
                        Room.class)
                .setParameter("ids", roomIds)
                .getResultList();
    }

    private List<Long> toLongIds(List<?> rawIds) {
        return rawIds.stream()
                .map(value -> ((Number) value).longValue())
                .toList();
    }

    private void appendAmenitiesFilter(StringBuilder sql, RoomSearchCriteria c) {
        List<Amenity> amenities = c.getAmenities();
        if (amenities == null || amenities.isEmpty()) {
            return;
        }

        for (int i = 0; i < amenities.size(); i++) {
            sql.append(" AND LOWER(COALESCE(CAST(r.amenities AS VARCHAR(1000)), '')) LIKE :amenity")
                    .append(i)
                    .append("\n");
        }
    }

    private void setAmenitiesParameters(Query query, RoomSearchCriteria c) {
        List<Amenity> amenities = c.getAmenities();
        if (amenities == null || amenities.isEmpty()) {
            return;
        }

        for (int i = 0; i < amenities.size(); i++) {
            query.setParameter("amenity" + i, "%" + normalizeAmenityForSearch(amenities.get(i)) + "%");
        }
    }

    private String normalizeAmenityForSearch(Amenity amenity) {
        return amenity.getDisplayName()
                .toLowerCase(Locale.ROOT);
    }
}
