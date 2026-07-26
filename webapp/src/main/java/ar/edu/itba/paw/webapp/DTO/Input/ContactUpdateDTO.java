package ar.edu.itba.paw.webapp.DTO.Input;

import ar.edu.itba.paw.model.swaps.SwapStatus;
import ar.edu.itba.paw.webapp.validation.ValidDateRange;
import ar.edu.itba.paw.webapp.validation.ValidEnum;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.xml.bind.annotation.XmlRootElement;
import java.time.LocalDate;

@XmlRootElement
@ValidDateRange(start = "checkIn", end = "checkOut", message = "{error.contact.dateRange}")
public class ContactUpdateDTO {

    @NotNull
    @ValidEnum(enumClass = SwapStatus.class)
    @Pattern(regexp = "(?i)ACCEPTED|REJECTED|CANCELED", message = "{validation.enum.invalid}")
    private String status;

    private LocalDate checkIn;

    private LocalDate checkOut;

    public ContactUpdateDTO() {
        // Required by Jersey/MOXy.
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDate checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDate checkOut) {
        this.checkOut = checkOut;
    }

    public SwapStatus getParsedStatus() {
        return SwapStatus.valueOf(status.toUpperCase());
    }
}
