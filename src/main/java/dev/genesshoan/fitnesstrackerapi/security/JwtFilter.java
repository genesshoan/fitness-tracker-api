package dev.genesshoan.fitnesstrackerapi.security;

import dev.genesshoan.fitnesstrackerapi.auth.service.JwtService;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.InvalidJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter that intercepts incoming HTTP requests and
 * populates the Spring Security context if a valid JWT is present.
 *
 * <p>
 * This filter:
 * <ul>
 * <li>Extracts the Bearer token from the Authorization header</li>
 * <li>Validates and parses the JWT</li>
 * <li>Loads user details from the database</li>
 * <li>Sets the authenticated user in the SecurityContext</li>
 * </ul>
 *
 * Requests targeting /auth endpoints are not explicitly excluded from
 * filtering; the filter only skips processing when no Bearer token is present.
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;

  /**
   * Intercepts each HTTP request once per request lifecycle.
   *
   * <p>
   * If a valid JWT is found in the Authorization header, the user is
   * authenticated and stored in the Spring Security context.
   *
   * @param request     incoming HTTP request
   * @param response    HTTP response
   * @param filterChain filter chain for continuing request processing
   * @throws ServletException in case of filter processing errors
   * @throws IOException      in case of I/O errors
   */
  @Override
  protected void doFilterInternal(
      @NotNull HttpServletRequest request,
      @NotNull HttpServletResponse response,
      @NotNull FilterChain filterChain) throws ServletException, IOException {

    String header = request.getHeader(HttpHeaders.AUTHORIZATION);

    // No token present, continue without authentication
    if (header == null || !header.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String jwtToken = header.substring(7);

    String username;

    try {
      username = jwtService.extractUsername(jwtToken);
    } catch (InvalidJwtException e) {
      SecurityContextHolder.clearContext();
      filterChain.doFilter(request, response);
      return;
    }

    // Set authentication only if context is empty
    if (SecurityContextHolder.getContext().getAuthentication() == null) {

      UserDetails userDetails = userDetailsService.loadUserByUsername(username);

      UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
          userDetails,
          null,
          userDetails.getAuthorities());

      authToken.setDetails(
          new WebAuthenticationDetailsSource().buildDetails(request));

      SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    filterChain.doFilter(request, response);
  }
}
