package ar.edu.itba.paw.model;

import javax.persistence.Embeddable;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Embeddable
public class DateRange {
    private LocalDate startDate;
    private LocalDate endDate;

    public DateRange() {
    //
    }

    public DateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("Start date must be before or equal to end date");
            }
        }

        this.startDate = startDate;
        this.endDate = endDate;
    }

    public DateRange(String startDate, String endDate) {
        this(startDate, endDate, "startDate", "endDate");
    }

    public DateRange(String startDate, String endDate, String startFieldName, String endFieldName) {
        this(parseDate(startDate, startFieldName), parseDate(endDate, endFieldName));
    }

    private static LocalDate parseDate(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Invalid ISO-8601 date for field '" + fieldName + "'. Expected yyyy-MM-dd.");
        }
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public int getAmountOfDays(){
        if (startDate == null || endDate == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        return (int) days;
    }

    @Override
    public String toString() {
        DateTimeFormatter withYear = DateTimeFormatter.ofPattern("d MMM yyyy");
        DateTimeFormatter noYear =DateTimeFormatter.ofPattern("d MMM");
        if (startDate.getYear() == endDate.getYear())
            return startDate.format(noYear) + " — " + endDate.format(withYear);
        else
            return startDate.format(withYear) + " — " + endDate.format(withYear);
    }

    public boolean isBeforeToday() {
        return endDate.isBefore(LocalDate.now());
    }

    public boolean isInBetween(LocalDate date) {
        return startDate.isBefore(date) && endDate.isAfter(date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startDate, endDate);
    }
}
