package ar.edu.itba.paw.model.token;

import ar.edu.itba.paw.model.User;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "verification_token")
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "verification_token_id_seq")
    @SequenceGenerator(sequenceName = "verification_token_id_seq", name = "verification_token_id_seq", allocationSize = 1)
    @Column
    private Long id;

    // Relación con User (muchos tokens pueden pertenecer a un usuario)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(length = 6, nullable = false, unique = true)
    private String token;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TokenType type;
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    /* Default*/ VerificationToken() {
        //Just for Hibernate
    }
    public VerificationToken(User user, String token, TokenType type, Instant expiryDate) {
        this.user = user;
        this.token = token;
        this.type = type;
        this.expiryDate = expiryDate;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setType(TokenType type) {
        this.type = type;
    }
    public TokenType getType() {
        return type;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

}
