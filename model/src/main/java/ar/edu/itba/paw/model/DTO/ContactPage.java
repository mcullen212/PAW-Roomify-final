package ar.edu.itba.paw.model.DTO;

import ar.edu.itba.paw.model.swaps.Contact;

import java.util.List;
import java.util.Map;

public class ContactPage {
    private final List<Contact> contacts;
    private final long totalItems;
    private final int currentPage;
    private final int pageSize;
    private final Map<Long, Boolean> pendingReviewByContactId;

    public ContactPage(List<Contact> contacts, long totalItems, int currentPage, int pageSize) {
        this(contacts, totalItems, currentPage, pageSize, Map.of());
    }

    public ContactPage(List<Contact> contacts, long totalItems, int currentPage, int pageSize,
                       Map<Long, Boolean> pendingReviewByContactId) {
        this.contacts = contacts;
        this.totalItems = totalItems;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.pendingReviewByContactId = pendingReviewByContactId;
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public boolean isReviewPending(long contactId) {
        return pendingReviewByContactId.getOrDefault(contactId, false);
    }

    public int getTotalPages() {
        if (pageSize <= 0 || totalItems <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalItems / pageSize);
    }
}
