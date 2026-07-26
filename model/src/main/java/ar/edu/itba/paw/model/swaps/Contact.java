package ar.edu.itba.paw.model.swaps;

import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.rooms.Room;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "contact")
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "contact_id_seq")
    @SequenceGenerator(sequenceName = "contact_id_seq", name = "contact_id_seq", allocationSize = 1)
    @Column(name = "id")
    private long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_room_id", nullable = false)
    private Room roomRequested;

    @Column(name = "contact_date", nullable = false)
    private LocalDateTime contactDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SwapStatus status;

    @Column(name = "is_swap", nullable = false)
    private boolean isSwap;
    @Column(name = "money_offer", precision = 10, scale = 2)
    private BigDecimal moneyOffer;   // null if it’s a swap

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_user_id")
    private User offerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_offer_id")
    private Room roomOffered;
    @Transient
    private boolean pendingReview;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "startDate", column = @Column(name = "requested_start_date", nullable = false)),
            @AttributeOverride(name = "endDate", column = @Column(name = "requested_end_date",nullable = false))
    })
    private DateRange requestedRange;   // offer start/end

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "startDate", column = @Column(name = "offered_start_date")),
            @AttributeOverride(name = "endDate", column = @Column(name = "offered_end_date"))
    })
    private DateRange offeredRange;   // requested start/end

    /* Default */ protected Contact() {}

    public Contact(Room roomRequested, LocalDateTime contactDate, SwapStatus status,
                   boolean isSwap, BigDecimal moneyOffer, User offerUser, Room roomOffered,
                   DateRange requestedRange, DateRange offeredRange) {
        this.roomRequested = roomRequested;
        this.contactDate = contactDate;
        this.isSwap = isSwap;
        this.moneyOffer = moneyOffer;
        this.offerUser = offerUser;
        this.status = status;
        this.roomOffered = roomOffered;
        this.requestedRange = requestedRange;
        this.offeredRange = offeredRange;
    }

    public Contact(long id, Room roomRequested, LocalDateTime contactDate, SwapStatus status,
                   boolean isSwap, BigDecimal moneyOffer, User offerUser, Room roomOffered, // <-- Usa User
                   DateRange requestedRange, DateRange offeredRange) {
        this.id = id;
        this.roomRequested = roomRequested;
        this.contactDate = contactDate;
        this.isSwap = isSwap;
        this.moneyOffer = moneyOffer;
        this.offerUser = offerUser;
        this.status = status;
        this.roomOffered = roomOffered;
        this.requestedRange = requestedRange;
        this.offeredRange = offeredRange;
    }

    public long getId() { return id; }
    public Room getRoomRequested() { return roomRequested; }
    public Room getRoomOffered() { return roomOffered; }
    public User getOfferUser() { return offerUser; }
    public long getOfferUserId() { return offerUser != null ? offerUser.getId() : 0; }
    public LocalDateTime getContactDate() { return contactDate; }
    public SwapStatus getStatus() { return status; }
    public DateRange getRequestedRange() { return requestedRange; }
    public DateRange getOfferedRange() { return offeredRange; }
    public boolean isSwap() { return isSwap; }
    public BigDecimal getMoneyOffer() { return moneyOffer; }
    public boolean isPendingReview() {
        return pendingReview;
    }
    public void setPendingReview(boolean pendingReview) {
        this.pendingReview = pendingReview;
    }
    public void setOfferedRange(DateRange newOfferedRange) {
        this.offeredRange = newOfferedRange;
    }
    public void setStatus(SwapStatus status) {
        this.status = status;
    }
    public boolean isOfferer(long userId) {
        return roomRequested.getOwner().getId() == userId;
    }
}

