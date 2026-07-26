package ar.edu.itba.paw.model.DTO;


import ar.edu.itba.paw.model.rooms.Review;
import ar.edu.itba.paw.model.swaps.Contact;

import java.util.List;

public class PastSwapResultsDTO {

    private final List<Contact> pastSwaps;
    private final List<Review> pastReviews;
    private final List<String> usersOffering;
    private final int currentPage;
    private final int totalPages;

    public PastSwapResultsDTO(List<Contact> pastSwaps,
                           List<Review> pastReviews,
                           List<String> usersOffering,
                           int currentPage,
                           int totalPages) {
        this.pastSwaps = pastSwaps;
        this.pastReviews = pastReviews;
        this.usersOffering = usersOffering;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
    }

    public List<Contact> getPastSwaps() {
        return pastSwaps;
    }

    public List<Review> getPastReviews() {
        return pastReviews;
    }

    public List<String> getUsersOffering() {
        return usersOffering;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }
}