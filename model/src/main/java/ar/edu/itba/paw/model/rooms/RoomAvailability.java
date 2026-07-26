package ar.edu.itba.paw.model.rooms;

import ar.edu.itba.paw.model.DateRange;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name="room_availability")
public class RoomAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "room_availability_id_seq")
    @SequenceGenerator(sequenceName = "room_availability_id_seq", name = "room_availability_id_seq", allocationSize = 1)
    @Column(name = "id")
    private long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)//foreign key to Room entity (?) foreignKey = @ForeignKey(name = "fk_availability_room"
    private Room room;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "startDate", column = @Column(name = "start_date", nullable = false)),
            @AttributeOverride(name = "endDate", column = @Column(name = "end_date", nullable = false))
    })
    private DateRange range;   // requested start/end

    /* Default*/ RoomAvailability() {
        //Just for Hibernate
    }

    public RoomAvailability(Room room, DateRange range) {
        this.room = room;
        this.range = range;
    }


    public RoomAvailability(long id, Room room, DateRange range) {
        this.id = id;
        this.room = room;
        this.range = range;
    }

    public Long getId() {
        return id;
    }

    public Long getRoomId() {
        return room.getId();
    }

    public Room getRoom() {
        return room;
    }

    public DateRange getRange() {
        return range;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, room != null ? room.getId() : null, range);
    }
}
