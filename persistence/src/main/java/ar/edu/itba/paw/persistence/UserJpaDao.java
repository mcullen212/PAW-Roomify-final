package ar.edu.itba.paw.persistence;
import ar.edu.itba.paw.interfaces.persistence.UserDao;
import ar.edu.itba.paw.model.User;
import org.springframework.stereotype.Repository;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.Locale;
import java.util.Optional;

@Repository
public class UserJpaDao implements UserDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<User> findUserById(long id){
        return Optional.ofNullable(em.find(User.class, id));
    }
    @Override
    public Optional<User> findUserByEmail(String email) {
        final TypedQuery<User> query = em.createQuery("FROM User as u WHERE u.email = :email", User.class);
        query.setParameter("email", email);
        return query.getResultList().stream().findAny();
    }
    @Override
    public User create(String name, String email, String hashPassword, Locale locale) {
        final User user = new User(email, name, hashPassword, false,locale.toString(), null, null);
        em.persist(user); //Se encarga de setearle al user la primary key.
        return user;
    }

    @Override
    public boolean changePassword(long userId, String newPassword) {
        return findUserById(userId).map(user -> {
            user.setPassword(newPassword);
            em.flush();
            return true;
        }).orElse(false);
    }

    @Override
    public boolean verifiedEmail(long userId) {
        return findUserById(userId).map(user -> {
            user.setVerified();
            return true;
        }).orElse(false);
    }
    @Override
    public boolean updateLocale(long userId, Locale locale) {
        return findUserById(userId).map(user -> {
            user.setLocale(locale);
            return true;
        }).orElse(false);
    }
    @Override
    public boolean updateProfile(long userId, String bio, String travelPrefs) {
        return findUserById(userId).map(user -> {
            if (bio != null) {
                user.setBio(bio);
            }
            if (travelPrefs != null) {
                user.setTravelPreferences(travelPrefs);
            }
            return true;
        }).orElse(false);
    }
}
