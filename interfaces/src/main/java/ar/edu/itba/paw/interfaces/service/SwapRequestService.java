package ar.edu.itba.paw.interfaces.service;

import ar.edu.itba.paw.model.swaps.Contact;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface SwapRequestService {
    Contact processSwapRequest(long roomRequestedId, LocalDate startDate, LocalDate endDate, Boolean isSwap,
            BigDecimal moneyOffer, // Usá los tipos de dato correctos
            Long roomOfferedId,
            Long tripId,
            String username
    );
}
