package dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.caffeine.Bucket4jCaffeine;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;

/**
 * Configures the in-memory store used to keep rate limit buckets.
 *
 * <p>Buckets live in a Caffeine cache local to the application instance, so
 * limits are not shared between instances and are reset on restart. For a
 * single-instance deployment this avoids an external dependency such as Redis.
 */
@Configuration
public class BucketConfig {

    private static final long MAX_BUCKETS = 100_000;
    private static final Duration KEEP_AFTER_FULL_REFILL = Duration.ofMinutes(10);

    /**
     * Caffeine evicts buckets under size pressure, and an evicted bucket is
     * rebuilt with a full capacity, so the cache must be big enough for the
     * expected number of distinct clients. Expiration is handled by Bucket4j
     * (Caffeine's own expireAfter* cannot be combined with its Expiry).
     *
     * @return a proxy manager backed by the Caffeine cache
     */
    @Bean
    ProxyManager<String> proxyManager() {
        return Bucket4jCaffeine.<String>builderFor(Caffeine.newBuilder().maximumSize(MAX_BUCKETS))
                .expirationAfterWrite(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(KEEP_AFTER_FULL_REFILL))
                .build();
    }
}
