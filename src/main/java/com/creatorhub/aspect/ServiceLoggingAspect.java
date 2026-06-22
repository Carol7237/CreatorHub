package com.creatorhub.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Cross-cutting DEBUG logging for the service layer: logs method entry, exit and
 * elapsed time around every {@code service.impl} method.
 *
 * <p>SECURITY: deliberately logs only the method name and the argument COUNT,
 * never argument values — some service methods receive a {@code UserRequest}
 * containing the plaintext password, which must never reach the logs.
 *
 * <p>Active mainly on the dev profile (where {@code com.creatorhub} is at DEBUG);
 * the {@code isDebugEnabled()} guard makes it a no-op otherwise.
 */
@Aspect
@Component
@Slf4j
public class ServiceLoggingAspect {

    @Around("execution(* com.creatorhub.service.impl..*(..))")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!log.isDebugEnabled()) {
            return joinPoint.proceed();
        }
        String method = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName();
        long startNanos = System.nanoTime();
        log.debug("call {}() [{} args]", method, joinPoint.getArgs().length);
        try {
            Object result = joinPoint.proceed();
            log.debug("done {}() in {} ms", method, elapsedMs(startNanos));
            return result;
        } catch (Throwable ex) {
            log.debug("fail {}() after {} ms: {}", method, elapsedMs(startNanos), ex.getClass().getSimpleName());
            throw ex;
        }
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
