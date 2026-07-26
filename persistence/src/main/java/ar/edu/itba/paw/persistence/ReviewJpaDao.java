package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.interfaces.persistence.ReviewDao;
import ar.edu.itba.paw.model.DTO.RoomReviewStats;
import ar.edu.itba.paw.model.swaps.Contact;
import ar.edu.itba.paw.model.rooms.Review;
import ar.edu.itba.paw.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ReviewJpaDao implements ReviewDao {
    @PersistenceContext
    private EntityManager em;
    public static final Logger LOGGER = LoggerFactory.getLogger(ReviewJpaDao.class);


    @Override
    public Review save(Contact contact, User reviewer, double rating, String comment, LocalDateTime createdAt) {
        final Review review = new Review(contact, reviewer, rating, comment, createdAt);
        em.persist(review);
        return review;
    }


    @Override
    public boolean existsByContactAndReviewer(Long contactId, Long reviewerId) {
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(r) FROM Review r WHERE r.contact.id = :contactId AND r.reviewer.id = :reviewerId",
                Long.class
        );
        query.setParameter("contactId", contactId);
        query.setParameter("reviewerId", reviewerId);
        return query.getSingleResult() > 0;
    }

    @Override
    public double roomRating(long roomId) {
        Query query = em.createNativeQuery("""
                SELECT AVG(r.rating)
                FROM review r
                JOIN contact c ON c.id = r.contact_id
                JOIN room requested_room ON requested_room.id = c.requested_room_id
                WHERE (
                    CASE
                        WHEN c.is_swap = TRUE AND requested_room.owner_id = r.reviewer_id THEN c.room_offer_id
                        ELSE c.requested_room_id
                    END
                ) = :roomId
                """)
                .setParameter("roomId", roomId);

        Number avgRating = (Number) query.getSingleResult();
        return avgRating != null ? avgRating.doubleValue() : 0.0;
    }

    @Override
    public Map<Long, RoomReviewStats> getReviewStatsByRoomIds(List<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Query query = em.createNativeQuery("""
                SELECT review_rooms.room_id, COUNT(review_rooms.review_id), AVG(review_rooms.rating)
                FROM (
                    SELECT
                        CASE
                            WHEN c.is_swap = TRUE AND requested_room.owner_id = r.reviewer_id THEN c.room_offer_id
                            ELSE c.requested_room_id
                        END AS room_id,
                        r.id AS review_id,
                        r.rating AS rating
                    FROM review r
                    JOIN contact c ON c.id = r.contact_id
                    JOIN room requested_room ON requested_room.id = c.requested_room_id
                ) review_rooms
                WHERE review_rooms.room_id IN (:roomIds)
                GROUP BY review_rooms.room_id
                """)
                .setParameter("roomIds", roomIds);

        Map<Long, RoomReviewStats> statsByRoomId = new HashMap<>();
        for (Object row : query.getResultList()) {
            Object[] columns = (Object[]) row;
            long roomId = ((Number) columns[0]).longValue();
            int totalReviews = ((Number) columns[1]).intValue();
            double averageRating = columns[2] == null ? 0 : ((Number) columns[2]).doubleValue();
            statsByRoomId.put(roomId, new RoomReviewStats(roomId, totalReviews, averageRating));
        }
        return statsByRoomId;
    }

    @Override
    public List<Review> findByReviewerId(long userId, int page, int pageSize) {
        final int offset = (page - 1) * pageSize;

        Query idQuery = em.createNativeQuery("""
                SELECT r.id
                FROM review r
                WHERE r.reviewer_id = :userId
                ORDER BY r.created_at DESC, r.id DESC
                LIMIT :limit OFFSET :offset
                """)
                .setParameter("userId", userId)
                .setParameter("limit", pageSize)
                .setParameter("offset", offset);
        List<Long> reviewIds = toLongIds(idQuery.getResultList());

        if (reviewIds.isEmpty()) {
            return Collections.emptyList();
        }

        return findReviewsByIds(reviewIds);
    }

    @Override
    public List<Review> findByRoomId(long roomId, int page, int pageSize){
        Query idQuery = em.createNativeQuery("""
                SELECT r.id
                FROM review r
                JOIN contact c ON c.id = r.contact_id
                JOIN room requested_room ON requested_room.id = c.requested_room_id
                WHERE (
                    CASE
                        WHEN c.is_swap = TRUE AND requested_room.owner_id = r.reviewer_id THEN c.room_offer_id
                        ELSE c.requested_room_id
                    END
                ) = :roomId
                ORDER BY r.created_at DESC, r.id DESC
                LIMIT :limit OFFSET :offset
                """)
                .setParameter("roomId", roomId)
                .setParameter("limit", pageSize)
                .setParameter("offset", (page - 1) * pageSize);
        List<Long> reviewIds = toLongIds(idQuery.getResultList());

        if (reviewIds.isEmpty()) {
            return Collections.emptyList();
        }

        return findReviewsByIds(reviewIds);
    }

    @Override
    public int getReviewsCountByRoomId(long roomId){
        Query countQuery = em.createNativeQuery("""
                SELECT COUNT(DISTINCT r.id)
                FROM review r
                JOIN contact c ON c.id = r.contact_id
                JOIN room requested_room ON requested_room.id = c.requested_room_id
                WHERE (
                    CASE
                        WHEN c.is_swap = TRUE AND requested_room.owner_id = r.reviewer_id THEN c.room_offer_id
                        ELSE c.requested_room_id
                    END
                ) = :roomId
                """)
                .setParameter("roomId", roomId);

        Number count = (Number) countQuery.getSingleResult();

        return count != null ? count.intValue() : 0;
    }

    @Override
    public boolean deleteReview(long reviewId){
        int deleted = em.createQuery("DELETE FROM Review r WHERE r.id = :reviewId")
                .setParameter("reviewId", reviewId)
                .executeUpdate();
        return deleted > 0;
    }

    @Override
    public Optional<Review> findReviewById(long id) {
        return Optional.ofNullable(em.find(Review.class, id));
    }

    @Override
    public List<Review> getReviewsByRoomOfOwner(long ownerId, int page, int pageSize) {
        final int offset = (page - 1) * pageSize;

        Query idQuery = em.createNativeQuery("""
                SELECT r.id
                FROM review r
                JOIN contact c ON c.id = r.contact_id
                JOIN room requested_room ON requested_room.id = c.requested_room_id
                JOIN room reviewed_room ON reviewed_room.id = (
                    CASE
                        WHEN c.is_swap = TRUE AND requested_room.owner_id = r.reviewer_id THEN c.room_offer_id
                        ELSE c.requested_room_id
                    END
                )
                WHERE reviewed_room.owner_id = :ownerId
                  AND r.reviewer_id <> :ownerId
                ORDER BY r.created_at DESC, r.id DESC
                LIMIT :limit OFFSET :offset
                """)
                .setParameter("ownerId", ownerId)
                .setParameter("limit", pageSize)
                .setParameter("offset", offset);
        List<Long> reviewIds = toLongIds(idQuery.getResultList());

        if (reviewIds.isEmpty()) {
            return Collections.emptyList();
        }

        return findReviewsByIds(reviewIds);
    }

    @Override
    public int countReviewsByRoomOwner(long ownerId) {
        Query countQuery = em.createNativeQuery("""
                SELECT COUNT(r.id)
                FROM review r
                JOIN contact c ON c.id = r.contact_id
                JOIN room requested_room ON requested_room.id = c.requested_room_id
                JOIN room reviewed_room ON reviewed_room.id = (
                    CASE
                        WHEN c.is_swap = TRUE AND requested_room.owner_id = r.reviewer_id THEN c.room_offer_id
                        ELSE c.requested_room_id
                    END
                )
                WHERE reviewed_room.owner_id = :ownerId
                  AND r.reviewer_id <> :ownerId
                """)
                .setParameter("ownerId", ownerId);
        return ((Number) countQuery.getSingleResult()).intValue();
    }

    @Override
    public double getAverageRatingByRoomOwner(long ownerId) {
        Query query = em.createNativeQuery("""
                SELECT AVG(r.rating)
                FROM review r
                JOIN contact c ON c.id = r.contact_id
                JOIN room requested_room ON requested_room.id = c.requested_room_id
                JOIN room reviewed_room ON reviewed_room.id = (
                    CASE
                        WHEN c.is_swap = TRUE AND requested_room.owner_id = r.reviewer_id THEN c.room_offer_id
                        ELSE c.requested_room_id
                    END
                )
                WHERE reviewed_room.owner_id = :ownerId
                  AND r.reviewer_id <> :ownerId
                """)
                .setParameter("ownerId", ownerId);

        Number avgRating = (Number) query.getSingleResult();
        return avgRating != null ? avgRating.doubleValue() : 0.0;
    }

    @Override
    public int countByReviewerId(long userId) {
        TypedQuery<Long> countQuery = em.createQuery(
                "SELECT COUNT(r.id) FROM Review r WHERE r.reviewer.id = :userId",
                Long.class
        );
        countQuery.setParameter("userId", userId);

        Long result = countQuery.getSingleResult();
        return result.intValue();
    }

    private List<Review> findReviewsByIds(List<Long> reviewIds) {
        return em.createQuery("""
                        SELECT r FROM Review r
                        JOIN FETCH r.reviewer
                        JOIN FETCH r.contact c
                        JOIN FETCH c.roomRequested
                        LEFT JOIN FETCH c.roomOffered
                        WHERE r.id IN :ids
                        ORDER BY r.createdAt DESC, r.id DESC
                        """, Review.class)
                .setParameter("ids", reviewIds)
                .getResultList();
    }

    private List<Long> toLongIds(List<?> rawIds) {
        return rawIds.stream()
                .map(value -> ((Number) value).longValue())
                .toList();
    }
}
