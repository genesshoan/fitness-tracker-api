package dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web;

import java.lang.reflect.Method;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import dev.genesshoan.fitnesstrackerapi.ratelimit.application.service.RateLimitService;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web.ip.ClientIpResolver;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * Intercepts methods annotated with {@link RateLimited} and charges the request
 * before the method runs.
 *
 * <p>The aspect runs with {@link Ordered#HIGHEST_PRECEDENCE}, so the limit is
 * checked as early as possible, before authentication or transaction work.
 * Because of that ordering it cannot bind the annotation as an advice parameter:
 * Spring's {@code ExposeInvocationInterceptor} would run later and the join point
 * match would not be available. The annotation is therefore read reflectively
 * from the target method with {@link AnnotatedElementUtils}, which also supports
 * composed annotations.
 *
 * <p>The aspect can be disabled with {@code app.rate-limit.enabled=false}; when
 * disabled it is not registered at all, so annotated methods behave as if the
 * annotation were absent.
 *
 * <p>Quota is consumed before {@link ProceedingJoinPoint#proceed()}, which means
 * failed attempts (wrong password, unknown email, rejected registration) still
 * count. This is intentional: it prevents brute-force attempts from being free.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "app.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class RateLimitAspect {

    private final ClientIpResolver ipResolver;
    private final RateLimitService rateLimitService;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * Consumes the request quota and then invokes the intercepted method.
     *
     * <p>The account key, when present, is evaluated as a SpEL expression against
     * the method arguments. It must resolve to a non-blank value: a configured
     * account limit is never skipped silently.
     *
     * @param pjp join point of the annotated method
     * @return the value returned by the intercepted method
     * @throws Throwable whatever the intercepted method throws, or the rate limit
     *     exception when a bucket is exhausted
     * @throws IllegalStateException if the annotation cannot be found, there is no
     *     HTTP request bound to the thread, or the account key resolves to blank
     */
    @Around("@annotation(RateLimited)")
    public Object check(ProceedingJoinPoint pjp) throws Throwable {
        Method method = resolveMethod(pjp);
        RateLimited rateLimited = AnnotatedElementUtils.findMergedAnnotation(method, RateLimited.class);
        if (rateLimited == null) {
            throw new IllegalStateException("@RateLimited not found on " + method);
        }

        String ip = ipResolver.resolve(currentRequest());
        String accountId = resolveAccountId(pjp, method, rateLimited);

        rateLimitService.consume(rateLimited.policy(), ip, accountId);

        return pjp.proceed();
    }

    /**
     * Resolves the most specific method targeted by the join point.
     *
     * @param pjp join point being advised
     * @return the implementation method, even when the proxy targets an interface
     */
    private Method resolveMethod(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        return AopUtils.getMostSpecificMethod(
                signature.getMethod(), pjp.getTarget().getClass());
    }

    /**
     * Returns the HTTP request bound to the current thread.
     *
     * @return the current servlet request
     * @throws IllegalStateException if the advice runs outside a servlet request
     */
    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            throw new IllegalStateException("@RateLimited can only be used inside an HTTP request");
        }
        return servletAttributes.getRequest();
    }

    /**
     * Evaluates the annotation's account key against the method arguments.
     *
     * @param pjp join point used to access the target and arguments
     * @param method target method used to discover parameter names
     * @param rateLimited annotation instance being processed
     * @return the resolved account identifier, or {@code null} when the annotation
     *     does not configure an account key
     * @throws IllegalStateException if a configured key resolves to {@code null} or
     *     blank
     */
    private String resolveAccountId(ProceedingJoinPoint pjp, Method method, RateLimited rateLimited) {
        String accountKey = rateLimited.accountKey().trim();
        if (accountKey.isEmpty()) {
            return null;
        }

        var context = new MethodBasedEvaluationContext(pjp.getTarget(), method, pjp.getArgs(), nameDiscoverer);

        String accountId = parser.parseExpression(accountKey).getValue(context, String.class);
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalStateException(
                    "Rate limit accountKey '%s' evaluated to an empty value".formatted(accountKey));
        }

        return accountId;
    }
}
