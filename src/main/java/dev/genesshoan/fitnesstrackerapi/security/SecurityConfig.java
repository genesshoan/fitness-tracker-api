package dev.genesshoan.fitnesstrackerapi.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

/**
 * Central security configuration for the application.
 *
 * <p>
 * This configuration defines:
 * <ul>
 * <li>Stateless authentication using JWT</li>
 * <li>Authorization rules for API endpoints</li>
 * <li>Integration of custom JWT filter into the Spring Security filter
 * chain</li>
 * <li>Password encoding strategy</li>
 * <li>Authentication manager exposure</li>
 * </ul>
 *
 * <p>
 * Security model:
 * <ul>
 * <li>/api/v1/auth/** → public endpoints (login, register, refresh,
 * logout)</li>
 * <li>All other endpoints require authentication via JWT</li>
 * <li>No server-side sessions are used (STATELESS)</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  /**
   * Custom JWT authentication filter that extracts and validates
   * JWT tokens from incoming requests and populates the SecurityContext.
   */
  private final JwtFilter jwtFilter;

  /**
   * Defines the Spring Security filter chain.
   *
   * <p>
   * Configures:
   * <ul>
   * <li>CSRF disabled (stateless API)</li>
   * <li>Public and secured endpoints</li>
   * <li>Stateless session management</li>
   * <li>JWT filter integration before UsernamePasswordAuthenticationFilter</li>
   * </ul>
   *
   * @param http HttpSecurity configuration object
   * @return configured SecurityFilterChain
   * @throws Exception in case of security configuration errors
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    return http
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/**").permitAll()

            .requestMatchers(HttpMethod.GET, "/api/v1/user/me").authenticated()
            .requestMatchers(HttpMethod.PUT, "/api/v1/user/me/**").authenticated()

            .anyRequest().authenticated())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  /**
   * Exposes the AuthenticationManager used for authentication operations
   * such as login via UsernamePasswordAuthenticationToken.
   *
   * @param config Spring AuthenticationConfiguration
   * @return AuthenticationManager instance
   * @throws Exception if authentication manager cannot be retrieved
   */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  /**
   * Password encoder used for hashing user passwords.
   *
   * <p>
   * BCrypt is used due to its adaptive hashing strength and industry adoption.
   *
   * @return BCryptPasswordEncoder instance
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
