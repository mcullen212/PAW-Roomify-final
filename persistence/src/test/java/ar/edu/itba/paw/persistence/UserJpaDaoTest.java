package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.User; // Assuming your User class is in ar.edu.itba.paw.model
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
import java.util.Locale;
import java.util.Optional;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class UserJpaDaoTest {
    // User Alice (ID 1): Verified Owner
    private static final long ALICE_ID = 1L;
    private static final String ALICE_EMAIL = "alice.owner@example.com";
    private static final String ALICE_NAME = "Alice Owner";
    private static final String ALICE_PASSWORD = "hashed_pass_1";

    // New User details (for register test)
    private static final String NEW_EMAIL = "test@newpawswap.com";
    private static final String NEW_PASSWORD_HASH = "new_pass_hash";
    private static final String NEW_NAME = "New User";
    private static final Locale NEW_LOCALE = Locale.ENGLISH;

    @Autowired
    private UserJpaDao userDao; // Injected JPA DAO (UserHibernateDao equivalent)

    @Autowired
    private DataSource ds;

    @PersistenceContext
    private EntityManager em;

    private JdbcTemplate jdbcTemplate;

    @Before
    public void setup() {
        jdbcTemplate = new JdbcTemplate(ds);
    }

    @Rollback
    @Test
    public void testCreate() {
        final User user = userDao.create(NEW_NAME, NEW_EMAIL, NEW_PASSWORD_HASH,NEW_LOCALE);

        em.flush();

        assertNotNull(user);
        assertEquals(NEW_NAME, user.getName());
        assertEquals(NEW_EMAIL, user.getEmail());
        assertEquals(NEW_PASSWORD_HASH, user.getPassword());
        assertEquals(NEW_LOCALE, user.getLocale());
        Assert.assertEquals(1, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "AppUser", String.format("email = '%s'", NEW_EMAIL)));
    }

    @Rollback
    @Test
    public void testGetById_UserExists() {
        final Optional<User> user = userDao.findUserById(ALICE_ID); // Using ID 1

        assertTrue(user.isPresent());
        assertEquals(ALICE_EMAIL, user.get().getEmail());
    }

    @Rollback
    @Test
    public void testGetById_UserDoesNotExist() {
        final Optional<User> user = userDao.findUserById(9999L); // Non-existent ID

        Assert.assertFalse(user.isPresent());
    }

    @Rollback
    @Test
    public void testGetByEmail_UserExists() {
        final Optional<User> user = userDao.findUserByEmail(ALICE_EMAIL); // Using Alice's email

        assertTrue(user.isPresent());
        assertEquals(ALICE_EMAIL, user.get().getEmail());
    }

    @Rollback
    @Test
    public void testGetByEmail_UserDoesNotExist() {
        final Optional<User> user = userDao.findUserByEmail("nonexistent@email.com");

        Assert.assertFalse(user.isPresent());
    }

    @Rollback
    @Test
    public void testChangePassword() {
        final String NEW_PASSWORD_HASH = "new password hash";

        // 2. Ejercitación (Exercise)
        final boolean result = userDao.changePassword(ALICE_ID, NEW_PASSWORD_HASH);

        // 3. Postcondiciones (Assert)
        assertTrue(result);

        em.flush();
        em.clear();

        final User persistedUser = em.find(User.class, ALICE_ID);

        assertNotNull(persistedUser);
        assertEquals(NEW_PASSWORD_HASH, persistedUser.getPassword());
    }

}
