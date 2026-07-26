package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.token.TokenType;
import ar.edu.itba.paw.model.token.VerificationToken;
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
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class VerificationTokenJpaDaoTest {
    // User Alice (ID 1): Verified Owner
    private static final long ALICE_ID = 1L; // Change password

    private static final long  TOKEN_ID_EMAIL = 1L;
    private static final long TOKEN_ID_EXPIRED = 2L;
    private static final String TOKEN_VALUE = "D5E6F7";

    @Autowired
    private DataSource ds;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private UserJpaDao userDao;

    @Autowired
    private VerificationTokenJpaDao tokenDao;

    private JdbcTemplate jdbcTemplate;

    private User user;
    private VerificationToken token;
    private VerificationToken tokenExpired;

    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(ds);
        user = em.find(User.class, ALICE_ID); // PREGUNTAR
        token = em.find(VerificationToken.class, TOKEN_ID_EMAIL);
        tokenExpired = em.find(VerificationToken.class, TOKEN_ID_EXPIRED);
    }

    @Rollback
    @Test
    public void testFindToken() {
        // 1. Setup

        // 2. Exercise
        Optional<VerificationToken> retrieved = tokenDao.findByToken(TOKEN_VALUE);

        // 3. Assert: token found correctly
        assertTrue(retrieved.isPresent());
        VerificationToken found = retrieved.get();
        assertEquals(TOKEN_VALUE, found.getToken());
        assertEquals(TokenType.RESET_PASSWORD, found.getType());
        assertEquals(user.getId(), found.getUser().getId());
        assertEquals(user.getEmail(), found.getUser().getEmail());
    }

    @Rollback
    @Test
    public void testFindByUser() {
        // 1. Setup

        // 2. Exercise
        Optional<VerificationToken> retrieved = tokenDao.findByUser(user.getId());

        // 3. Assert
        assertTrue(retrieved.isPresent());
        assertEquals(TOKEN_VALUE, retrieved.get().getToken());
    }

    @Rollback
    @Test
    public void testDelete() {
        // 2. Exercise
        boolean deleted = tokenDao.delete(TOKEN_VALUE);

        // 3. Assert
        assertTrue(deleted);
        assertEquals(0, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "verification_token", String.format("id = '%d'", TOKEN_ID_EMAIL)));
    }

    @Rollback
    @Test
    public void testDeleteExpiredTokens() {
        // 1. Setup

        // 2. Exercise
        boolean deleted = tokenDao.deleteExpiredTokens();

        // 3. Assert
        assertTrue(deleted);
        assertEquals(0, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "verification_token", String.format("id = '%d'", TOKEN_ID_EXPIRED)));
    }
}

