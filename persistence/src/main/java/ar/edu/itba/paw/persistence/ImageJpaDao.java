package ar.edu.itba.paw.persistence;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import ar.edu.itba.paw.interfaces.persistence.ImageDao;
import ar.edu.itba.paw.model.Image;
import ar.edu.itba.paw.model.User;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class ImageJpaDao implements ImageDao{

    @PersistenceContext
    private EntityManager em;

    public Image insert(String filename, String contentType, int sizeBytes, byte[] data){
        Image image = new Image(filename, contentType, sizeBytes, data);
        em.persist(image);
        return image;
    }

    @Override
    public Image insert(String filename, String contentType, int sizeBytes, byte[] data, long ownerId) {
        User owner = em.find(User.class, ownerId);
        Image image = new Image(filename, contentType, sizeBytes, data, owner, LocalDateTime.now());
        em.persist(image);
        return image;
    }

    public Optional<Image> findImageById(long imageId){
        return Optional.ofNullable(em.find(Image.class, imageId));
    }

    @Override
    public boolean deleteImage(long imageId){
        int deleted = em.createQuery("DELETE FROM Image i WHERE i.id = :id")
                .setParameter("id", imageId)
                .executeUpdate();
        return deleted > 0;
    }

    @Override
    public boolean deleteIfUnassigned(long imageId) {
        int deleted = em.createNativeQuery("""
                DELETE FROM image i
                WHERE i.id = :id
                  AND NOT EXISTS (
                      SELECT 1 FROM room r WHERE r.image_id = i.id
                  )
                """)
                .setParameter("id", imageId)
                .executeUpdate();
        return deleted > 0;
    }

    @Override
    public int deleteUnassignedImagesOlderThan(LocalDateTime threshold) {
        return em.createNativeQuery("""
                DELETE FROM image i
                WHERE i.owner_id IS NOT NULL
                  AND i.created_at < :threshold
                  AND NOT EXISTS (
                      SELECT 1 FROM room r WHERE r.image_id = i.id
                  )
                """)
                .setParameter("threshold", threshold)
                .executeUpdate();
    }
}
