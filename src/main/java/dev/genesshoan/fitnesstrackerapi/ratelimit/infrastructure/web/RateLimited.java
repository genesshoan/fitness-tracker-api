package dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import dev.genesshoan.fitnesstrackerapi.ratelimit.domain.RateLimitPolicy;

/**
 * Marks a controller method whose requests must be rate limited.
 *
 * <p>The annotation is processed by {@link RateLimitAspect}, which charges the
 * client IP and, optionally, an account identifier before the method executes.
 * Limits come from the configured {@link #policy()}; the annotation only selects
 * the policy and the optional account key.
 *
 * @see RateLimitAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {

    /**
     * Policy whose limits apply to the request.
     *
     * @return the configured policy
     */
    RateLimitPolicy policy();

    /**
     * SpEL expression that resolves the account identifier from the method
     * arguments, for example {@code "#dto.email"}.
     *
     * <p>When empty, only the IP dimension is enforced. When configured, the
     * expression must resolve to a non-blank value or the request is rejected.
     *
     * @return the SpEL expression, or an empty string to skip the account limit
     */
    String accountKey() default "";
}
