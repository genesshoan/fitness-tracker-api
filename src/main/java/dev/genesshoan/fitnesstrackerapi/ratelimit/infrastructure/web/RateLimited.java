package dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import dev.genesshoan.fitnesstrackerapi.ratelimit.domain.RateLimitPolicy;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {
    RateLimitPolicy policy();

    String accountKey() default "";
}
