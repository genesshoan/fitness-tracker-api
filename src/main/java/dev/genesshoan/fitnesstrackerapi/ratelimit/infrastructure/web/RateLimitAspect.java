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

    private Method resolveMethod(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        return AopUtils.getMostSpecificMethod(
                signature.getMethod(), pjp.getTarget().getClass());
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            throw new IllegalStateException("@RateLimited can only be used inside an HTTP request");
        }
        return servletAttributes.getRequest();
    }

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
