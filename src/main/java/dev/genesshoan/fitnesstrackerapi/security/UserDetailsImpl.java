package dev.genesshoan.fitnesstrackerapi.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserDetailsImpl implements UserDetails {

    private final UUID userId;
    private final String identifier;
    private final String email;
    private final String username;
    private final String passwordHash;
    private final boolean emailVerified;
    private final Collection<? extends GrantedAuthority> authorities;

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.copyOf(authorities);
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return identifier;
    }

    public UUID getId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayUsername() {
        return username;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    @Override
    public String toString() {
        return "UserDetailsImpl[id=" + userId
                + ", identifier=" + identifier
                + ", username=" + username
                + ", email=***"
                + ", passwordHash=***"
                + ", emailVerified=" + emailVerified
                + ", authorities=" + authorities + "]";
    }
}
