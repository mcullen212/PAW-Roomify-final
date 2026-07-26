package ar.edu.itba.paw.interfaces.service;

import ar.edu.itba.paw.model.DTO.PastSwapResultsDTO;

import java.time.LocalDate;

public interface PastSwapsService {

    PastSwapResultsDTO getPastSwapsPage(String username,
                                        LocalDate today,
                                        int page,
                                        int pageSize);
}
