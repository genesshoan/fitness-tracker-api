package dev.genesshoan.fitnesstrackerapi.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.genesshoan.fitnesstrackerapi.common.error.handler.ProblemDetailUtils;
import lombok.RequiredArgsConstructor;

/**
 * Central security configuration for the application.
 *
 * <p>
 * This configuration defines:
 * <ul>
 * <li>Stateless authentication using JWT</li>
 * <li>Authorization rules for API endpoints</li>
 * <li>Cross-origin requests handled by the {@link CorsProperties} configuration</li>
 * <li>Integration of custom JWT filter into the Spring Security filter
 * chain</li>
 * <li>Password encoding strategy</li>
 * <li>Authentication manager exposure</li>
 * </ul>
 *
 * <p>
 * Security model:
 * <ul>
 * <li>/api/v1/auth/register, /verify-email, /resend-verification, /login,
 * /forgot-password and /reset-password → public endpoints</li>
 * <li>/api/v1/auth/refresh and /api/v1/auth/logout → cookie based session
 * endpoints, they are authorized by the refresh token cookie and are protected
 * against CSRF</li>
 * <li>Every other /api/v1/auth/** endpoint (e.g. PATCH /api/v1/auth/password)
 * → requires a valid JWT access token</li>
 * <li>Exercise catalog writes (POST/PUT/DELETE /api/v1/exercises/**) →
 * ADMIN role only</li>
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
     * Jackson object mapper used to serialize authentication failure responses.
     */
    private final ObjectMapper objectMapper;

    /**
     * Defines the Spring Security filter chain.
     *
     * <p>
     * Configures:
     * <ul>
     * <li>CSRF protection restricted to the cookie based session endpoints</li>
     * <li>Cross-origin requests through the configured {@link CorsProperties}</li>
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
        return http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new EagerCsrfTokenRequestHandler())
                        .requireCsrfProtectionMatcher(cookieSessionEndpoints()))
                .cors(cors -> {})
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/verify-email")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/resend-verification")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/forgot-password")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/reset-password")
                        .permitAll()

                        // Cookie based session endpoints, authorized by the refresh token cookie
                        // and protected against CSRF
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh")
                        .permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/auth/logout")
                        .permitAll()

                        // Every other authentication endpoint requires a valid access token
                        .requestMatchers("/api/v1/auth/**")
                        .authenticated()

                        // Documentation
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/v3/api-docs")
                        .permitAll()

                        // User endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/user/me")
                        .authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/user/me/**")
                        .authenticated()

                        // Exercise catalog
                        .requestMatchers(HttpMethod.GET, "/api/v1/muscles/**")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/exercises/**")
                        .authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/exercises/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/exercises/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/exercises/**")
                        .hasRole("ADMIN")

                        // Routine management
                        .requestMatchers("/api/v1/routines/**")
                        .authenticated()

                        // Progress Record
                        .requestMatchers("/api/v1/progress/**")
                        .authenticated()

                        // Statistics
                        .requestMatchers("/api/v1/stats/**")
                        .authenticated()

                        // WorkoutSession management
                        .requestMatchers("/api/v1/sessions/**")
                        .authenticated()

                        // Everything else
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                    objectMapper.writeValue(
                            response.getOutputStream(),
                            ProblemDetailUtils.errorResponse(
                                    HttpStatus.UNAUTHORIZED,
                                    "Unauthorized",
                                    "Authentication is required to access this resource",
                                    null,
                                    request));
                }))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Matches the session endpoints that rely on the refresh token cookie and
     * therefore are the only ones exposed to cross site request forgery.
     *
     * @return the request matcher of the cookie based session endpoints
     */
    private static RequestMatcher cookieSessionEndpoints() {
        PathPatternRequestMatcher.Builder builder = PathPatternRequestMatcher.withDefaults();

        return new OrRequestMatcher(
                builder.matcher(HttpMethod.POST, "/api/v1/auth/refresh"),
                builder.matcher(HttpMethod.DELETE, "/api/v1/auth/logout"));
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
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
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
