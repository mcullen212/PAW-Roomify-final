package ar.edu.itba.paw.model.swaps;

public enum SwapStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELED,
    EXPIRED;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
