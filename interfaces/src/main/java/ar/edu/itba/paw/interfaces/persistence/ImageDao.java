package ar.edu.itba.paw.interfaces.persistence;

import ar.edu.itba.paw.model.Image;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ImageDao {
    Image insert(String filename, String contentType, int sizeBytes, byte[] data);
    Image insert(String filename, String contentType, int sizeBytes, byte[] data, long ownerId);
    Optional<Image> findImageById(long imageId);
    //void delete(long roomId, long imageId);
    boolean deleteImage(long imageId);
    boolean deleteIfUnassigned(long imageId);
    int deleteUnassignedImagesOlderThan(LocalDateTime threshold);
}
