package ar.edu.itba.paw.persistence;
import ar.edu.itba.paw.interfaces.persistence.VerificationTokenDao;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.token.VerificationToken;
import ar.edu.itba.paw.model.token.TokenType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Repository
public class VerificationTokenJpaDao implements VerificationTokenDao {
    @PersistenceContext
    private EntityManager em;

    @Override
    public VerificationToken save(User user, String token, TokenType type, Instant expiryDate) {
        VerificationToken verificationToken = new VerificationToken(user, token, type, expiryDate);
        em.persist(verificationToken);
        return verificationToken;
    }

    @Override
    public Optional<VerificationToken> findByToken(String token) {
        TypedQuery<VerificationToken> query = em.createQuery(
                "FROM VerificationToken t JOIN FETCH t.user WHERE t.token = :tokenValue",
                VerificationToken.class
        );
        query.setParameter("tokenValue", token);
        return query.getResultList().stream().findFirst();
    }

    @Override
    public Optional<VerificationToken> findByUser(long userId){
            TypedQuery<VerificationToken> query = em.createQuery(
                    "FROM VerificationToken t JOIN FETCH t.user u WHERE u.id = :userId",
                    VerificationToken.class
            );
            query.setParameter("userId", userId);
            return query.getResultList().stream().findFirst();
    }

    @Override
    public boolean delete(String token) {
        int deleted = em.createQuery("DELETE FROM VerificationToken t WHERE t.token = :tokenValue")
                .setParameter("tokenValue", token)
                .executeUpdate();
        return deleted > 0;
    }

    @Override
    public boolean deleteExpiredTokens(){
        int deleted = em.createQuery("DELETE FROM VerificationToken t WHERE t.expiryDate < :now")
                .setParameter("now", Instant.now())
                .executeUpdate();
        return deleted > 0;
    }
}
