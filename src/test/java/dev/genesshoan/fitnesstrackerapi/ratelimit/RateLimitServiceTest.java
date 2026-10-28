package dev.genesshoan.fitnesstrackerapi.ratelimit;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.github.benmanes.caffeine.cache.Caffeine;
import dev.genesshoan.fitnesstrackerapi.ratelimit.application.exception.RateLimitExceededException;
import dev.genesshoan.fitnesstrackerapi.ratelimit.application.service.RateLimitService;
import dev.genesshoan.fitnesstrackerapi.ratelimit.domain.RateLimitPolicy;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.config.RateLimitProperties;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.config.RateLimitProperties.LimitConfig;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.config.RateLimitProperties.PolicyConfig;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.config.RateLimitProperties.RefillType;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web.ip.IpNormalizer;
import io.github.bucket4j.caffeine.Bucket4jCaffeine;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Unit Tests - Rate Limit Service")
class RateLimitServiceTest {

    private static final String IP = "203.0.113.7";
    private static final String OTHER_IP = "198.51.100.9";
    private static final RateLimitPolicy POLICY = RateLimitPolicy.LOGIN;
    private static final Duration ONE_HOUR = Duration.ofHours(1);

    @Test
    @DisplayName("rejects requests once the IP capacity is exhausted")
    void rejectsWhenIpCapacityIsExhausted() {
        var service = newService(ipOnly(limit(2, ONE_HOUR)));

        service.consume(POLICY, IP, null);
        service.consume(POLICY, IP, null);

        assertThatThrownBy(() -> service.consume(POLICY, IP, null))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessage("Too many requests");
    }

    @Test
    @DisplayName("keeps the limit per IP address")
    void keepsLimitPerIp() {
        var service = newService(ipOnly(limit(1, ONE_HOUR)));

        service.consume(POLICY, IP, null);

        assertThatCode(() -> service.consume(POLICY, OTHER_IP, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("normalizes the account so case and blanks share a bucket")
    void normalizesAccountIdentifier() {
        var service = newService(withAccount(limit(100, ONE_HOUR), limit(1, ONE_HOUR)));

        service.consume(POLICY, IP, "user@example.com");

        assertThatThrownBy(() -> service.consume(POLICY, IP, "  USER@Example.COM  "))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("limits each account independently")
    void limitsEachAccountIndependently() {
        var service = newService(withAccount(limit(100, ONE_HOUR), limit(1, ONE_HOUR)));

        service.consume(POLICY, IP, "a@example.com");

        assertThatCode(() -> service.consume(POLICY, IP, "b@example.com")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ignores the account limit when no account is given")
    void ignoresAccountLimitWhenNoAccount() {
        var service = newService(withAccount(limit(100, ONE_HOUR), limit(1, ONE_HOUR)));

        service.consume(POLICY, IP, null);
        service.consume(POLICY, IP, "   ");

        assertThatCode(() -> service.consume(POLICY, IP, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("enforces every configured bandwidth, not only the first one")
    void enforcesEveryBandwidth() {
        var service =
                newService(withAccount(limit(100, ONE_HOUR), limit(5, Duration.ofMinutes(1)), limit(2, ONE_HOUR)));

        service.consume(POLICY, IP, "a@example.com");
        service.consume(POLICY, IP, "a@example.com");

        assertThatThrownBy(() -> service.consume(POLICY, IP, "a@example.com"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("charges invalid addresses to a shared fallback bucket so they cannot be used to bypass limits")
    void invalidIpsShareFallbackBucket() {
        var service = newService(ipOnly(limit(1, ONE_HOUR)));

        service.consume(POLICY, "not-an-ip", null);

        assertThatThrownBy(() -> service.consume(POLICY, "another-invalid-value", null))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("reports the remaining wait in whole seconds")
    void reportsRetryAfterInSeconds() {
        var service = newService(ipOnly(limit(1, ONE_HOUR)));

        service.consume(POLICY, IP, null);

        assertThatThrownBy(() -> service.consume(POLICY, IP, null))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(exception -> assertThat(((RateLimitExceededException) exception).getRetryAfterSeconds())
                        .isBetween(3599L, 3600L));
    }

    @Test
    @DisplayName("does nothing when the policy has no limits")
    void doesNothingWithoutLimits() {
        var service = newService(new PolicyConfig(null, null));

        service.consume(POLICY, IP, "user@example.com");
        service.consume(POLICY, IP, "user@example.com");
    }

    @Test
    @DisplayName("fails loudly when the policy is missing from configuration")
    void failsWhenPolicyIsMissing() {
        var service = newService();

        assertThatThrownBy(() -> service.consume(POLICY, IP, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(POLICY.name());
    }

    private static RateLimitService newService(PolicyConfig... policies) {
        ProxyManager<String> proxyManager = Bucket4jCaffeine.<String>builderFor(
                        Caffeine.newBuilder().maximumSize(1_000))
                .expirationAfterWrite(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10)))
                .build();

        Map<RateLimitPolicy, PolicyConfig> configured = policies.length == 0 ? Map.of() : Map.of(POLICY, policies[0]);

        RateLimitProperties properties = new RateLimitProperties(configured, 64);
        return new RateLimitService(properties, proxyManager, new IpNormalizer(properties));
    }

    private static PolicyConfig ipOnly(LimitConfig ip) {
        return new PolicyConfig(List.of(ip), null);
    }

    private static PolicyConfig withAccount(LimitConfig ip, LimitConfig... account) {
        return new PolicyConfig(List.of(ip), List.of(account));
    }

    private static LimitConfig limit(int capacity, Duration period) {
        return new LimitConfig(capacity, period, RefillType.INTERVALLY);
    }
}
