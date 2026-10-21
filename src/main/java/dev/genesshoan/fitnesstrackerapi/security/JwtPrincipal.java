package dev.genesshoan.fitnesstrackerapi.security;

import java.util.UUID;

/**
 * Principal of a request authenticated with an access token.
 *
 * <p>The access token is stateless: it carries the user id and the authorities, so
 * the principal only holds the id. The controllers read the id from it through the
 * {@code @CurrentUserId} annotation, which resolves the {@code id} property of the
 * principal, and the services load the data they need.
 *
 * @param id the id of the authenticated user
 */
public record JwtPrincipal(UUID id) {}
