package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Image;
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
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class ImageJpaDaoTest {
    private static final long ASSOCIATED_IMAGE_ID = 1L;
    private static final long UNASSIGNED_IMAGE_ID = 3L;
    private static final long OWNER_ID = 1L;

    @Autowired
    private ImageJpaDao imageDao;

    @Autowired
    private DataSource ds;

    @PersistenceContext
    private EntityManager em;

    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(ds);
    }

    @Rollback
    @Test
    public void deleteIfUnassignedDoesNotDeleteAssociatedImage() {
        assertFalse(imageDao.deleteIfUnassigned(ASSOCIATED_IMAGE_ID));
        em.flush();

        assertEquals(1, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "image", "id = " + ASSOCIATED_IMAGE_ID));
    }

    @Rollback
    @Test
    public void deleteIfUnassignedDeletesUnassignedImage() {
        assertTrue(imageDao.deleteIfUnassigned(UNASSIGNED_IMAGE_ID));
        em.flush();

        assertEquals(0, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "image", "id = " + UNASSIGNED_IMAGE_ID));
    }

    @Rollback
    @Test
    public void deleteUnassignedImagesOlderThanDeletesOnlyOwnedOldUnassignedImages() {
        Image image = imageDao.insert("old.jpg", "image/jpeg", 3, new byte[]{1, 2, 3}, OWNER_ID);
        em.flush();
        jdbcTemplate.update("UPDATE image SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusDays(3)),
                image.getId());
        em.clear();

        int deleted = imageDao.deleteUnassignedImagesOlderThan(LocalDateTime.now().minusDays(1));

        assertEquals(1, deleted);
        em.flush();

        assertEquals(0, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "image", "id = " + image.getId()));
        assertEquals(1, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "image", "id = " + ASSOCIATED_IMAGE_ID));
    }
}
