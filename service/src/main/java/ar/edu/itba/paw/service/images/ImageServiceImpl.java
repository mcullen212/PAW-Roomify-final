package ar.edu.itba.paw.service.images;

import ar.edu.itba.paw.interfaces.exceptions.BusinessException;
import ar.edu.itba.paw.interfaces.exceptions.UserNotFoundException;
import ar.edu.itba.paw.interfaces.persistence.ImageDao;
import ar.edu.itba.paw.interfaces.persistence.RoomDao;
import ar.edu.itba.paw.interfaces.service.ImageService;
import ar.edu.itba.paw.interfaces.service.UserService;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.DTO.ImageDTO;
import org.springframework.stereotype.Service;
import ar.edu.itba.paw.model.Image;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
//import ar.edu.itba.paw.persistence.RoomImageJdbcDao;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Service
public class ImageServiceImpl implements ImageService {

    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");
    private final ImageDao imageDao;
    private final UserService userService;
    private final RoomDao roomDao;

    public ImageServiceImpl(ImageDao imageDao, UserService userService, RoomDao roomDao) {
        this.imageDao = imageDao;
        this.userService = userService;
        this.roomDao = roomDao;
    }

    private String sanitize(String name) {
        return (name == null || name.isBlank()) ? "image" : name.replaceAll("[\\r\\n\\t\\\\/]+","_");
    }

    @Transactional
    @Override
    public Image upload(ImageDTO image) {
        if (image == null || image.getData() == null || image.getSizeBytes() <= 0) {
            throw new BusinessException("Image is required");
        }
        if (image.getContentType() == null || !ALLOWED.contains(image.getContentType())) {
            throw new BusinessException("Invalid image content type");
        }

        return imageDao.insert(
                sanitize(image.getFilename()),
                image.getContentType(),
                image.getSizeBytes(),
                image.getData()
        );
    }

    @Transactional
    @Override
    public Image uploadRoomImage(String email, ImageDTO image) {
        if (image == null || image.getData() == null || image.getSizeBytes() <= 0) {
            throw new BusinessException("Image is required");
        }
        if (image.getContentType() == null || !ALLOWED.contains(image.getContentType())) {
            throw new BusinessException("Invalid image content type");
        }

        User owner = userService.findUserByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        return imageDao.insert(
                sanitize(image.getFilename()),
                image.getContentType(),
                image.getSizeBytes(),
                image.getData(),
                owner.getId()
        );
    }

    @Override
    public Optional<Image> findImageById(long imageId){
        return imageDao.findImageById(imageId);
    }

    @Override
    public Optional<Image> findOwnedUnassignedImage(long imageId, long ownerId) {
        return imageDao.findImageById(imageId)
                .filter(image -> image.getOwner() != null && image.getOwner().getId() == ownerId)
                .filter(image -> !roomDao.existsByImageId(imageId));
    }

    /*@Override
    public void delete(long roomId, long imageId){
        imageDao.delete(roomId, imageId);
    }*/

    @Transactional
    @Override
    public boolean deleteImage(long imageId){
        return imageDao.deleteImage(imageId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public boolean deleteIfUnassigned(long imageId) {
        return imageDao.deleteIfUnassigned(imageId);
    }

    @Transactional
    @Override
    public int deleteUnassignedImagesOlderThan(Duration age) {
        if (age == null || age.isNegative() || age.isZero()) {
            throw new BusinessException("Cleanup age must be positive");
        }
        return imageDao.deleteUnassignedImagesOlderThan(LocalDateTime.now().minus(age));
    }
}
