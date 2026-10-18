package dev.genesshoan.fitnesstrackerapi.security;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import dev.genesshoan.fitnesstrackerapi.user.application.port.in.GetUserCredentialsUseCase;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final GetUserCredentialsUseCase getUserCredentialsUseCase;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        var credentials = getUserCredentialsUseCase
                .findByIdentifier(identifier)
                .orElseThrow(() -> new UsernameNotFoundException(identifier));

        return new UserDetailsImpl(
                credentials.id(),
                identifier,
                credentials.email(),
                credentials.username(),
                credentials.passwordHash(),
                credentials.emailVerified(),
                List.of(new SimpleGrantedAuthority("ROLE_" + credentials.role())));
    }
}
