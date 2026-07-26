package ar.edu.itba.paw.interfaces.service;

import ar.edu.itba.paw.model.DTO.PublicUserProfileStats;
import ar.edu.itba.paw.model.DTO.UserProfileStats;

public interface ProfileService {
    UserProfileStats getPrivateProfile(long userId);
    PublicUserProfileStats getPublicProfile(long userId);
}
