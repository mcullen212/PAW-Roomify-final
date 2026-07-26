package ar.edu.itba.paw.model.rooms;

import ar.edu.itba.paw.model.swaps.Contact;
import ar.edu.itba.paw.model.User;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Entity
@Table(name = "review")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "review_id_seq")
    @SequenceGenerator(sequenceName = "review_id_seq", name = "review_id_seq", allocationSize = 1)
    @Column(name = "id")
    private long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @Column(nullable = false)
    private double rating;

    @Column
    private String comment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /* Default */ Review() {
        // Just for Hibernate
    }
    public Review(Contact contact, User reviewer, double rating, String comment, LocalDateTime createdAt) {
        this.contact = contact;
        this.reviewer = reviewer;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public Review(long id, Contact contact, User reviewer, double rating, String comment, LocalDateTime createdAt) {
        this.id = id;
        this.contact = contact;
        this.reviewer = reviewer;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public Contact getContact() {
        return contact;
    }

    @Transient
    public long getContactId() {
        return contact != null ? contact.getId() : 0;
    }
    public User getReviewer() {
        return reviewer;
    }

    public double getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public String getFormattedDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return this.createdAt.format(formatter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                contact != null ? contact.getId() : null,
                reviewer != null ? reviewer.getId() : null,
                reviewer != null ? reviewer.getName() : null,
                rating,
                comment,
                createdAt
        );
    }
}
