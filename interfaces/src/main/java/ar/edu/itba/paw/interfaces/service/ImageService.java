package ar.edu.itba.paw.interfaces.service;
import ar.edu.itba.paw.model.DTO.ImageDTO;
import ar.edu.itba.paw.model.Image;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.Optional;

public interface ImageService {


    Image upload(ImageDTO image);
    Image uploadRoomImage(String email, ImageDTO image);

    Optional<Image> findImageById(long imageId);
    Optional<Image> findOwnedUnassignedImage(long imageId, long ownerId);
        //void delete(long roomId, long imageId);

    boolean deleteImage(long imageId);
    boolean deleteIfUnassigned(long imageId);
    int deleteUnassignedImagesOlderThan(Duration age);
}
