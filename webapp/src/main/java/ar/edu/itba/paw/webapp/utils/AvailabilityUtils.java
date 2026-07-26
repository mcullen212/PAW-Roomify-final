package ar.edu.itba.paw.webapp.utils;

import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.rooms.RoomAvailability;
import ar.edu.itba.paw.webapp.view.AvailabilityView;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

public final class AvailabilityUtils {

    private AvailabilityUtils() {
        // no se instancia
    }

    public static List<AvailabilityView> toViewList(List<RoomAvailability> availabilities) {
        DateTimeFormatter fmt = DateTimeFormatter
                .ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(LocaleContextHolder.getLocale());

        return availabilities.stream()
                .map(a -> new AvailabilityView(
                        a.getId(),
                        a.getRange().getStartDate().format(fmt),
                        a.getRange().getEndDate().format(fmt)
                ))
                .toList();
    }

    public static RoomAvailability first(List<RoomAvailability> availabilities) {
        return availabilities.getFirst();
    }

    public static DateRange globalRange(List<RoomAvailability> availabilities) {
        if (availabilities == null || availabilities.isEmpty()) {
            return null;
        }

        LocalDate min = availabilities.stream()
                .map(a -> a.getRange().getStartDate())
                .min(LocalDate::compareTo)
                .orElse(null);

        LocalDate max = availabilities.stream()
                .map(a -> a.getRange().getEndDate())
                .max(LocalDate::compareTo)
                .orElse(null);

        if (min == null || max == null) {
            return null;
        }

        return new DateRange(min, max);
    }
}