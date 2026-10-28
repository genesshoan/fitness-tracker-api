package dev.genesshoan.fitnesstrackerapi.ratelimit;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import dev.genesshoan.fitnesstrackerapi.ratelimit.application.service.RateLimitService;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web.RateLimitAspect;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web.ip.ClientIpResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("Unit Tests - Rate limit feature flag")
class RateLimitEnabledFlagTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(ClientIpResolver.class, () -> mock(ClientIpResolver.class))
            .withBean(RateLimitService.class, () -> mock(RateLimitService.class))
            .withUserConfiguration(RateLimitAspect.class);

    @Test
    @DisplayName("registers the aspect when the flag is missing so existing deployments keep working")
    void registersAspectWhenFlagIsMissing() {
        runner.run(context -> assertThat(context).hasSingleBean(RateLimitAspect.class));
    }

    @Test
    @DisplayName("registers the aspect when the flag is enabled")
    void registersAspectWhenEnabled() {
        runner.withPropertyValues("app.rate-limit.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(RateLimitAspect.class));
    }

    @Test
    @DisplayName("does not register the aspect when the flag is disabled")
    void skipsAspectWhenDisabled() {
        runner.withPropertyValues("app.rate-limit.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(RateLimitAspect.class));
    }
}
