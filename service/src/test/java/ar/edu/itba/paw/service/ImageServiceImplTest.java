package ar.edu.itba.paw.service;

import ar.edu.itba.paw.interfaces.exceptions.BusinessException;
import ar.edu.itba.paw.interfaces.persistence.ImageDao;
import ar.edu.itba.paw.interfaces.persistence.RoomDao;
import ar.edu.itba.paw.interfaces.service.UserService;
import ar.edu.itba.paw.model.DTO.ImageDTO;
import ar.edu.itba.paw.model.Image;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.service.images.ImageServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ImageServiceImplTest {
    private static final long USER_ID = 1L;
    private static final long IMAGE_ID = 10L;
    private static final String EMAIL = "owner@test.com";

    @Mock
    private ImageDao mockImageDao;
    @Mock
    private UserService mockUserService;
    @Mock
    private RoomDao mockRoomDao;

    @InjectMocks
    private ImageServiceImpl imageService;

    private final User owner = new User(USER_ID, EMAIL, "Owner", "pass", true, Locale.ENGLISH.toString(), null, null);
    private final ImageDTO validImage = new ImageDTO("room.jpg", "image/jpeg", 3, new byte[]{1, 2, 3});
    private final Image ownedImage = new Image(IMAGE_ID, "room.jpg", "image/jpeg", 3, new byte[]{1, 2, 3}, owner, LocalDateTime.now());

    @Test
    public void uploadRoomImageCreatesImageWithOwner() {
        when(mockUserService.findUserByEmail(EMAIL)).thenReturn(Optional.of(owner));
        when(mockImageDao.insert(eq("room.jpg"), eq("image/jpeg"), eq(3), any(byte[].class), eq(USER_ID))).thenReturn(ownedImage);

        Image result = imageService.uploadRoomImage(EMAIL, validImage);

        assertSame(ownedImage, result);
        verify(mockImageDao).insert(eq("room.jpg"), eq("image/jpeg"), eq(3), any(byte[].class), eq(USER_ID));
    }

    @Test
    public void uploadRoomImageRejectsInvalidContentType() {
        ImageDTO invalidImage = new ImageDTO("room.gif", "image/gif", 3, new byte[]{1, 2, 3});

        assertThrows(BusinessException.class, () -> imageService.uploadRoomImage(EMAIL, invalidImage));

        verify(mockImageDao, never()).insert(anyString(), anyString(), anyInt(), any(), anyLong());
    }

    @Test
    public void findOwnedUnassignedImageReturnsImageForOwnerWhenUnused() {
        when(mockImageDao.findImageById(IMAGE_ID)).thenReturn(Optional.of(ownedImage));
        when(mockRoomDao.existsByImageId(IMAGE_ID)).thenReturn(false);

        Optional<Image> result = imageService.findOwnedUnassignedImage(IMAGE_ID, USER_ID);

        assertTrue(result.isPresent());
        assertSame(ownedImage, result.get());
    }

    @Test
    public void findOwnedUnassignedImageRejectsAssignedImage() {
        when(mockImageDao.findImageById(IMAGE_ID)).thenReturn(Optional.of(ownedImage));
        when(mockRoomDao.existsByImageId(IMAGE_ID)).thenReturn(true);

        assertTrue(imageService.findOwnedUnassignedImage(IMAGE_ID, USER_ID).isEmpty());
    }

    @Test
    public void deleteIfUnassignedDelegatesToDao() {
        when(mockImageDao.deleteIfUnassigned(IMAGE_ID)).thenReturn(true);

        assertTrue(imageService.deleteIfUnassigned(IMAGE_ID));
    }

    @Test
    public void deleteUnassignedImagesOlderThanUsesThreshold() {
        when(mockImageDao.deleteUnassignedImagesOlderThan(any(LocalDateTime.class))).thenReturn(2);

        int deleted = imageService.deleteUnassignedImagesOlderThan(Duration.ofDays(1));

        assertEquals(2, deleted);
        verify(mockImageDao).deleteUnassignedImagesOlderThan(any(LocalDateTime.class));
    }
}
