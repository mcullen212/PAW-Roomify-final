package ar.edu.itba.paw.model.trip;

import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.User;

import javax.persistence.*;
import java.util.Locale;

@Entity
@Table(name = "group_trip")
public class GroupTrip {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "group_trip_id_seq")
    @SequenceGenerator(sequenceName = "group_trip_id_seq", name = "group_trip_id_seq", allocationSize = 1)
    @Column(name = "id")
    private long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_user", nullable = false)
    private User owner;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "startDate", column = @Column(name = "start_date")),
            @AttributeOverride(name = "endDate", column = @Column(name = "end_date"))
    })
    private DateRange dateRange;

    @Enumerated(EnumType.STRING)
    @Column(name= "status")
    private TripStatus  status;

    protected GroupTrip() {}

    public GroupTrip(User owner, String title, DateRange dateRange) {
        this.owner = owner;
        this.title = title;
        this.dateRange = dateRange;
        this.status = TripStatus.PLANNING;
    }

    public long getId() { return id; }
    public User getOwner() { return owner; }
    public String getTitle() { return title; }
    public DateRange getDateRange() { return dateRange; }

    public void setDateRange(DateRange dateRange) { this.dateRange = dateRange; }

    public TripStatus getStatus() { return status; }
    public void setStatus(TripStatus status) { this.status = status; }

}