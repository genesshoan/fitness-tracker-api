package dev.genesshoan.fitnesstrackerapi.ratelimit;

import java.time.Duration;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import dev.genesshoan.fitnesstrackerapi.ratelimit.domain.RateLimitPolicy;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.config.RateLimitProperties;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.config.RateLimitProperties.RefillType;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web.ip.ClientIpProperties;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web.ip.IpProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit Tests - Rate Limit Properties")
class RateLimitPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfig.class)
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class));

    @Test
    @DisplayName("binds the policies under app.rate-limit and the provider under app.security")
    void bindsConfiguredProperties() {
        runner.withPropertyValues(
                        "app.security.ip-provider=CLOUDFLARE",
                        "app.rate-limit.ipv6-prefix=64",
                        "app.rate-limit.policies.LOGIN.ip[0].capacity=20",
                        "app.rate-limit.policies.LOGIN.ip[0].period=1m",
                        "app.rate-limit.policies.LOGIN.account[0].capacity=5",
                        "app.rate-limit.policies.LOGIN.account[0].period=15m")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    RateLimitProperties properties = context.getBean(RateLimitProperties.class);
                    assertThat(properties.ipv6Prefix()).isEqualTo(64);
                    assertThat(properties.policies()).containsOnlyKeys(RateLimitPolicy.LOGIN);

                    var policy = properties.policies().get(RateLimitPolicy.LOGIN);
                    assertThat(policy.ip()).singleElement().satisfies(ip -> {
                        assertThat(ip.capacity()).isEqualTo(20);
                        assertThat(ip.period()).isEqualTo(Duration.ofMinutes(1));
                        assertThat(ip.refill()).isEqualTo(RefillType.GREEDY);
                    });
                    assertThat(policy.account()).singleElement().satisfies(account -> {
                        assertThat(account.capacity()).isEqualTo(5);
                        assertThat(account.period()).isEqualTo(Duration.ofMinutes(15));
                    });

                    assertThat(context.getBean(ClientIpProperties.class).ipProvider())
                            .isEqualTo(IpProvider.CLOUDFLARE);
                });
    }

    @Test
    @DisplayName("rejects a policy with zero capacity")
    void rejectsZeroCapacity() {
        runner.withPropertyValues(
                        "app.security.ip-provider=CLOUDFLARE",
                        "app.rate-limit.policies.LOGIN.ip[0].capacity=0",
                        "app.rate-limit.policies.LOGIN.ip[0].period=1m")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("rejects a configuration without policies")
    void rejectsMissingPolicies() {
        runner.withPropertyValues("app.security.ip-provider=CLOUDFLARE")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("rejects an IPv6 prefix outside the valid range")
    void rejectsInvalidIpv6Prefix() {
        runner.withPropertyValues(
                        "app.security.ip-provider=CLOUDFLARE",
                        "app.rate-limit.ipv6-prefix=200",
                        "app.rate-limit.policies.LOGIN.ip[0].capacity=1",
                        "app.rate-limit.policies.LOGIN.ip[0].period=1m")
                .run(context -> assertThat(context).hasFailed());
    }

    @EnableConfigurationProperties({RateLimitProperties.class, ClientIpProperties.class})
    static class PropertiesConfig {}
}
