package ar.edu.itba.paw.webapp.validation;
import ar.edu.itba.paw.webapp.DTO.Input.RoomAvailabilityDTO;
import ar.edu.itba.paw.webapp.DTO.Input.RoomInputDTO;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Comparator;
import java.util.List;
import java.time.LocalDate;
import java.util.stream.Collectors;

public class NoOverlappingDateRangesValidator
        implements ConstraintValidator<NoOverlappingDateRanges, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        List<DateRangeBounds> ranges = extractRanges(value);

        if (ranges == null || ranges.isEmpty()) {
            return true; // @NotNull o @Size se encarga si querés exigir que haya al menos uno
        }

        // Filtrar solo los que tienen ambas fechas y ordenarlos por startDate
        List<DateRangeBounds> sorted = ranges.stream()
                .filter(r -> r != null && r.getStartDate() != null && r.getEndDate() != null)
                .sorted(Comparator.comparing(DateRangeBounds::getStartDate))
                .collect(Collectors.toList());

        for (int i = 1; i < sorted.size(); i++) {
            DateRangeBounds prev = sorted.get(i - 1);
            DateRangeBounds curr = sorted.get(i);

            LocalDate prevEnd   = prev.getEndDate();
            LocalDate currStart = curr.getStartDate();

            // Si el inicio actual NO es posterior al fin anterior, se solapan
            // (cambia a currStart.isBefore(prevEnd) si querés permitir compartir día)
            if (!currStart.isAfter(prevEnd)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("{room.error.overlappingDates}")
                        .addPropertyNode(value instanceof RoomInputDTO ? "dateRange" : "dateRangeForms")
                        .addConstraintViolation();
                return false;
            }
        }

        return true;
    }

    private List<DateRangeBounds> extractRanges(Object value) {
        if (value instanceof RoomInputDTO) {
            List<RoomAvailabilityDTO> dateRange = ((RoomInputDTO) value).getDateRange();
            if (dateRange == null) return null;
            return dateRange.stream()
                    .map(dto -> new DateRangeBounds(dto.getStartDate(), dto.getEndDate()))
                    .collect(Collectors.toList());
        }
        return null;
    }

    private static class DateRangeBounds {
        private final LocalDate startDate;
        private final LocalDate endDate;

        private DateRangeBounds(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        private LocalDate getStartDate() {
            return startDate;
        }

        private LocalDate getEndDate() {
            return endDate;
        }
    }
}
