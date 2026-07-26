package ar.edu.itba.paw.webapp.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class PawUserDetails extends User {
    private final ar.edu.itba.paw.model.User user;

    public PawUserDetails(ar.edu.itba.paw.model.User user, Collection<? extends GrantedAuthority> authorities) {
        super(user.getEmail(), user.getPassword(), authorities);
        this.user = user;
    }

    public long getId(){
        return user.getId();
    }

    public ar.edu.itba.paw.model.User getUser() {
        return user;
    }
}
