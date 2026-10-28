package dev.genesshoan.fitnesstrackerapi.ratelimit.web;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.github.benmanes.caffeine.cache.Caffeine;
import dev.genesshoan.fitnesstrackerapi.ratelimit.application.exception.RateLimitExceededException;
import dev.genesshoan.fitnesstrackerapi.ratelimit.application.service.RateLimitService;
import dev.genesshoan.fitnesstrackerapi.ratelimit.domain.RateLimitPolicy;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.config.RateLimitProperties;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.config.RateLimitProperties.LimitConfig;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.config.RateLimitProperties.PolicyConfig;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.config.RateLimitProperties.RefillType;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web.RateLimitAspect;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web.RateLimited;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web.ip.ClientIpResolver;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web.ip.IpNormalizer;
import io.github.bucket4j.caffeine.Bucket4jCaffeine;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Unit Tests - Rate Limit Aspect")
class RateLimitAspectTest {

    private static final String IP = "203.0.113.7";
    private static final Duration ONE_HOUR = Duration.ofHours(1);

    private ClientIpResolver ipResolver;
    private AnnotationConfigApplicationContext context;
    private StubController target;

    @BeforeEach
    void setUp() {
        ipResolver = mock(ClientIpResolver.class);
        when(ipResolver.resolve(any())).thenReturn(IP);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        target = new StubController();
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("resolves the account key with SpEL and charges the normalized value")
    void resolvesAccountKeyWithSpel() {
        StubController controller = controller(withAccount(limit(1, ONE_HOUR)));

        assertThat(controller.login(new LoginDto("User@Example.com"))).isEqualTo("token");

        assertThatThrownBy(() -> controller.login(new LoginDto(" user@example.COM ")))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("uses the request IP resolved by the resolver")
    void usesResolvedIp() {
        StubController controller = controller(withAccount(limit(1, ONE_HOUR)));

        controller.login(new LoginDto("a@example.com"));

        verify(ipResolver).resolve(any());
    }

    @Test
    @DisplayName("does not invoke the target method when the limit is exceeded")
    void doesNotInvokeTargetWhenLimited() {
        StubController controller = controller(withAccount(limit(1, ONE_HOUR)));

        controller.login(new LoginDto("a@example.com"));

        assertThatThrownBy(() -> controller.login(new LoginDto("a@example.com")))
                .isInstanceOf(RateLimitExceededException.class);
        assertThat(target.invocations).isEqualTo(1);
    }

    @Test
    @DisplayName("fails closed when a configured account key resolves to null")
    void failsClosedOnNullAccountKey() {
        StubController controller = controller(withAccount(limit(1, ONE_HOUR)));

        assertThatThrownBy(() -> controller.login(new LoginDto(null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("accountKey");
    }

    @Test
    @DisplayName("fails when there is no HTTP request bound to the thread")
    void failsWithoutHttpRequest() {
        StubController controller = controller(withAccount(limit(1, ONE_HOUR)));
        RequestContextHolder.resetRequestAttributes();

        assertThatThrownBy(() -> controller.login(new LoginDto("a@example.com")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP request");
    }

    @Test
    @DisplayName("only applies the IP limit when the annotation has no account key")
    void onlyAppliesIpLimitWithoutAccountKey() {
        StubController controller = controller(withAccount(limit(1, ONE_HOUR)));

        controller.loginWithoutAccount("a@example.com");
        controller.loginWithoutAccount("b@example.com");

        assertThat(target.invocations).isEqualTo(2);
    }

    @Test
    @DisplayName("consumes quota even when the target method fails")
    void consumesQuotaWhenTargetFails() {
        target.failure = new IllegalStateException("authentication failed");
        StubController controller = controller(withAccount(limit(1, ONE_HOUR)));

        assertThatThrownBy(() -> controller.login(new LoginDto("a@example.com")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("authentication failed");

        assertThatThrownBy(() -> controller.login(new LoginDto("a@example.com")))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("lets the call through when the policy has no limits")
    void letsCallThroughWithoutLimits() {
        StubController controller = controller(new PolicyConfig(null, null));

        assertThatCode(() -> controller.login(new LoginDto("a@example.com"))).doesNotThrowAnyException();
    }

    @SuppressWarnings("unchecked")
    private StubController controller(PolicyConfig policy) {
        ProxyManager<String> proxyManager = Bucket4jCaffeine.<String>builderFor(
                        Caffeine.newBuilder().maximumSize(1_000))
                .expirationAfterWrite(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10)))
                .build();

        RateLimitProperties properties = new RateLimitProperties(Map.of(RateLimitPolicy.LOGIN, policy), 64);
        RateLimitService rateLimitService =
                new RateLimitService(properties, proxyManager, new IpNormalizer(properties));

        context = new AnnotationConfigApplicationContext();
        context.registerBean(ClientIpResolver.class, () -> ipResolver);
        context.registerBean(RateLimitService.class, () -> rateLimitService);
        context.registerBean(StubController.class, () -> target);
        context.registerBean(RateLimitAspect.class, () -> new RateLimitAspect(ipResolver, rateLimitService));
        context.register(AspectConfig.class);
        context.refresh();

        return context.getBean(StubController.class);
    }

    private static PolicyConfig withAccount(LimitConfig... account) {
        return new PolicyConfig(List.of(limit(100, ONE_HOUR)), List.of(account));
    }

    private static LimitConfig limit(int capacity, Duration period) {
        return new LimitConfig(capacity, period, RefillType.INTERVALLY);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    static class AspectConfig {}

    public record LoginDto(String email) {}

    public static class StubController {

        private int invocations;
        private RuntimeException failure;

        @RateLimited(policy = RateLimitPolicy.LOGIN, accountKey = "#dto.email")
        public String login(LoginDto dto) {
            invocations++;
            if (failure != null) {
                throw failure;
            }
            return "token";
        }

        @RateLimited(policy = RateLimitPolicy.LOGIN)
        public String loginWithoutAccount(String email) {
            invocations++;
            return "token";
        }
    }
}
