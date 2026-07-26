package ar.edu.itba.paw.webapp.DTO.Input;

import ar.edu.itba.paw.webapp.validation.ValidDateRange;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.xml.bind.annotation.XmlRootElement;
import java.math.BigDecimal;
import java.time.LocalDate;

@XmlRootElement
@ValidDateRange(start = "checkIn", end = "checkOut", message = "{error.contact.dateRange}")
public class ContactInputDTO {

    @NotNull
    @Positive
    private Long roomRequestedId;

    @NotNull
    private LocalDate checkIn;

    @NotNull
    private LocalDate checkOut;

    @NotNull
    private Boolean isSwap;

    private BigDecimal dayPrice;

    private Long roomOfferedId;

    public ContactInputDTO() {
        // Required by Jersey/MOXy.
    }

    public Long getRoomRequestedId() {
        return roomRequestedId;
    }

    public void setRoomRequestedId(Long roomRequestedId) {
        this.roomRequestedId = roomRequestedId;
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

    public Boolean getIsSwap() {
        return isSwap;
    }

    public void setIsSwap(Boolean swap) {
        isSwap = swap;
    }

    public BigDecimal getDayPrice() {
        return dayPrice;
    }

    public void setDayPrice(BigDecimal dayPrice) {
        this.dayPrice = dayPrice;
    }

    public Long getRoomOfferedId() {
        return roomOfferedId;
    }

    public void setRoomOfferedId(Long roomOfferedId) {
        this.roomOfferedId = roomOfferedId;
    }
}
