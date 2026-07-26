package ar.edu.itba.paw.model.trip;

import ar.edu.itba.paw.model.DateRange;

import javax.persistence.*;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

@Entity
@Table(name = "trip")
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trip_id_seq")
    @SequenceGenerator(sequenceName = "trip_id_seq", name = "trip_id_seq", allocationSize = 1)
    @Column(name = "id")
    private long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_group_trip", nullable = false)
    private GroupTrip groupTrip; // Mapea id_group_trip a GroupTrip

    @Column(name = "country", length = 100, nullable = false)
    private String country;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "startDate", column = @Column(name = "start_date", nullable = false)),
            @AttributeOverride(name = "endDate", column = @Column(name = "end_date",nullable = false))
    })
    private DateRange dateRange;

    /* Default constructor for Hibernate */
    protected Trip() {}

    public Trip(GroupTrip groupTrip, String country, DateRange dateRange) {
        this.groupTrip = groupTrip;
        this.country = country;
        this.dateRange = dateRange;
    }

    public long getId() { return id; }
    public GroupTrip getGroupTrip() { return groupTrip; }
    public String getCountry() { return country; }
    public DateRange getDateRange() { return dateRange; }

    public String getCountryCode(){
        if (this.country == null || this.country.isEmpty()) {
            return null;
        }

        Optional<String> countryCode = Arrays.stream(Locale.getISOCountries())
                .map(code -> new Locale("", code))
                .filter(locale -> this.country.trim().equalsIgnoreCase(locale.getDisplayCountry(Locale.ENGLISH)))
                .map(Locale::getCountry)
                .findFirst();
        return countryCode.orElse(null);
    }

}