package dev.genesshoan.fitnesstrackerapi.ratelimit.application.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import dev.genesshoan.fitnesstrackerapi.ratelimit.application.exception.RateLimitExceededException;
import dev.genesshoan.fitnesstrackerapi.ratelimit.domain.RateLimitPolicy;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.config.RateLimitProperties;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.config.RateLimitProperties.LimitConfig;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.config.RateLimitProperties.PolicyConfig;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.config.RateLimitProperties.RefillType;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web.ip.IpNormalizer;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;

/**
 * Enforces the rate limits configured for each {@link RateLimitPolicy}.
 *
 * <p>A request is checked against two independent dimensions:
 * <ul>
 * <li>the <b>client IP</b>, which protects the whole endpoint from floods; and</li>
 * <li>the <b>account identifier</b> (normally the email in the body), which
 * protects a single account even when the attacker rotates IP addresses.</li>
 * </ul>
 *
 * <p>Buckets are stored through a {@link ProxyManager} keyed by policy, subject
 * and normalized identifier. Every configured bandwidth of a subject must have
 * capacity; the first exhausted one raises a {@link RateLimitExceededException}.
 *
 * <p>Checking is fail-closed: a missing policy, an invalid account key or an
 * unparseable IP never bypasses the limit.
 */
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RateLimitProperties properties;
    private final ProxyManager<String> proxyManager;
    private final IpNormalizer normalizer;

    /** Dimensions on which a request is charged. */
    private enum RateLimitSubject {
        IP,
        ACCOUNT
    }

    /**
     * Consumes one unit from every bucket that applies to the request.
     *
     * <p>The IP bucket is always charged. The account bucket is charged only when
     * {@code accountId} is present and not blank; accounts are compared after
     * trimming and lower-casing so the same address always maps to one bucket.
     *
     * @param policy policy whose configured limits apply to the endpoint
     * @param ip raw client IP, normalized by {@link IpNormalizer} before use
     * @param accountId account identifier to charge, or {@code null} to skip the
     *     account dimension
     * @throws RateLimitExceededException if any configured bucket has no capacity
     * @throws IllegalStateException if the policy is not present in the configuration
     */
    public void consume(RateLimitPolicy policy, String ip, String accountId) {
        PolicyConfig policyConfig = properties.policies().get(policy);

        if (policyConfig == null) {
            throw new IllegalStateException("No rate limit policy configured for " + policy);
        }

        check(policy, RateLimitSubject.IP, ip, policyConfig.ip());

        if (accountId != null && !accountId.isBlank()) {
            check(policy, RateLimitSubject.ACCOUNT, accountId.trim().toLowerCase(Locale.ROOT), policyConfig.account());
        }
    }

    /**
     * Charges one request against the buckets of a single subject.
     *
     * <p>Does nothing when the subject has no configured limits. The bucket key
     * combines the policy, the subject and the normalized identifier so different
     * policies and dimensions never share capacity.
     *
     * @param policy policy being enforced
     * @param subject whether the identifier is an IP or an account
     * @param identifier value used to build the bucket key
     * @param limits bandwidths configured for the subject
     * @throws RateLimitExceededException if the bucket has no capacity left
     */
    private void check(RateLimitPolicy policy, RateLimitSubject subject, String identifier, List<LimitConfig> limits) {

        if (limits == null || limits.isEmpty()) {
            return;
        }

        String normalizedIdentifier =
                switch (subject) {
                    case RateLimitSubject.IP -> normalizer.normalize(identifier);
                    case RateLimitSubject.ACCOUNT -> identifier;
                };

        String key = policy.name() + ":" + subject.name() + ":" + normalizedIdentifier;

        Bucket bucket = proxyManager.builder().build(key, () -> buildConfiguration(limits));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long seconds = Math.max(1, (long) Math.ceil(probe.getNanosToWaitForRefill() / 1_000_000_000.0));
            throw new RateLimitExceededException(seconds);
        }
    }

    /**
     * Builds the Bucket4j configuration for one subject.
     *
     * <p>Every configured bandwidth is added, so a subject can combine several
     * windows (for example {@code 1/minute} plus {@code 3/hour}). A limit refills
     * continuously ({@link RefillType#GREEDY}) or in full at the end of the period
     * ({@link RefillType#INTERVALLY}).
     *
     * @param limits bandwidths to add
     * @return the resulting bucket configuration
     */
    private BucketConfiguration buildConfiguration(List<LimitConfig> limits) {
        var builder = BucketConfiguration.builder();

        for (LimitConfig l : limits) {
            builder.addLimit(bw -> {
                var cap = bw.capacity(l.capacity());

                return l.refill() == RefillType.GREEDY
                        ? cap.refillGreedy(l.capacity(), l.period())
                        : cap.refillIntervally(l.capacity(), l.period());
            });
        }
        return builder.build();
    }
}
