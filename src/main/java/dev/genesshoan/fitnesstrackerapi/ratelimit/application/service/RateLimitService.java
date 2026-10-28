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

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RateLimitProperties properties;
    private final ProxyManager<String> proxyManager;
    private final IpNormalizer normalizer;

    private enum RateLimitSubject {
        IP,
        ACCOUNT
    }

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
