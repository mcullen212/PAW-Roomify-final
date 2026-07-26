package ar.edu.itba.paw.webapp.security;

import ar.edu.itba.paw.interfaces.service.UserService;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.webapp.config.PawUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;

@Component
public class PawUserDetailsService implements UserDetailsService {

    @Autowired
    private UserService us;

    private User loggedUser;

    @Override
    public UserDetails loadUserByUsername(final String email)
            throws UsernameNotFoundException {

        loggedUser = us.findUserByEmail(email).orElseThrow(
                () -> new UsernameNotFoundException("No user found with email: " + email)
        );

        final Collection<GrantedAuthority> authorities = new ArrayList<>();

        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        if (loggedUser.isVerified()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_VERIFIED_USER"));
        }

        return new PawUserDetails(loggedUser, authorities);
    }

    public User getUser() {
        return loggedUser;
    }
}