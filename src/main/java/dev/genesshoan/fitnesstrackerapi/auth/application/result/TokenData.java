package dev.genesshoan.fitnesstrackerapi.auth.application.result;

import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;

public record TokenData(
        UUID subject, Collection<? extends GrantedAuthority> authorities, Date issuedAt, Date expiresAt) {}
