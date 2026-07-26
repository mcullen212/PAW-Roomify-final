package ar.edu.itba.paw.model.trip;

import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.swaps.Contact;

import javax.persistence.*;

@Entity
@Table(name= "trip_contact")
public class TripContact {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trip_contact_id_seq")
    @SequenceGenerator(name = "trip_contact_id_seq", sequenceName = "trip_contact_id_seq", allocationSize = 1)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id_involved", nullable = false)
    private Room room;

    protected TripContact() {}

    public TripContact(Trip trip, Contact contact, Room room) {
        this.trip = trip;
        this.contact = contact;
        this.room = room;
    }

    public long getId() { return id; }
    public Trip getTrip() { return trip; }
    public Contact getContact() { return contact; }
    public Room getRoom() { return room; }
}
