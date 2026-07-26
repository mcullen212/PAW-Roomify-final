package ar.edu.itba.paw.webapp.view;

public class AvailabilityView {
    private final long id;
    private final String start;   // dd/MM/yyyy (o “12 de oct de 2025”)
    private final String end;

    public AvailabilityView(long id, String start, String end) {
        this.id = id;
        this.start = start;
        this.end = end;
    }
    public long getId() { return id; }
    public String getStart() { return start; }
    public String getEnd() { return end; }
}
