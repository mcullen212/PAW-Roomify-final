package ar.edu.itba.paw.model.DTO;

import java.util.List;

public class GroupTripPage {
    private final List<GroupTripListItem> items;
    private final int totalItems;
    private final int totalPages;

    public GroupTripPage(List<GroupTripListItem> items, int totalItems, int pageSize) {
        this.items = items;
        this.totalItems = totalItems;
        this.totalPages = pageSize <= 0 ? 0 : (int) Math.ceil((double) totalItems / pageSize);
    }

    public List<GroupTripListItem> getItems() {
        return items;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
