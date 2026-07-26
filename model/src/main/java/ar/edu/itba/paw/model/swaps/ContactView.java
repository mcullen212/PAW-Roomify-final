package ar.edu.itba.paw.model.swaps;

import java.util.Locale;

public enum ContactView {
    SENT,
    RECEIVED,
    ACTIVE,
    CANCELED,
    PAST,
    EXPIRED;

    public static ContactView fromQueryParam(String value) {
        if (value == null || value.isBlank()) {
            return SENT;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "sent":
            case "requested":
                return SENT;
            case "received":
            case "recieved":
                return RECEIVED;
            case "active":
                return ACTIVE;
            case "canceled":
            case "cancelled":
                return CANCELED;
            case "past":
                return PAST;
            case "expired":
                return EXPIRED;
            default:
                throw new IllegalArgumentException("Invalid contacts view: " + value);
        }
    }
}
