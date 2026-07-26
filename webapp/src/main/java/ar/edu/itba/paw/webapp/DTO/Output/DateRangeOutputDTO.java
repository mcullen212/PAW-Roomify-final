package ar.edu.itba.paw.webapp.DTO.Output;

import ar.edu.itba.paw.model.DateRange;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@XmlRootElement
public class DateRangeOutputDTO {
    private String startDate;
    private String endDate;

    public DateRangeOutputDTO() {
        // Required by Jersey/MOXy.
    }

    public DateRangeOutputDTO(final DateRange dateRange) {
        this.startDate = dateRange.getStartDate().toString();
        this.endDate = dateRange.getEndDate().toString();
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startDate, endDate);
    }
}
