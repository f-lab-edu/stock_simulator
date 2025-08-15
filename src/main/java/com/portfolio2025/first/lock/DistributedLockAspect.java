package com.portfolio2025.first.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.*;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAspect {

    private final RedissonClient redissonClient;

    private final SpelExpressionParser parser = new SpelExpressionParser(); // 표현식 파싱
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer(); // 메서드 파라미터명 확인

    @Around("@annotation(com.portfolio2025.first.lock.DistributedLock)")
    public Object lock(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        DistributedLock lockAnnotation = method.getAnnotation(DistributedLock.class);

        String prefix = lockAnnotation.prefix();
        String keyExpression = lockAnnotation.key();
        long waitTime = lockAnnotation.waitTime();
        long leaseTime = lockAnnotation.leaseTime();
        TimeUnit timeUnit = lockAnnotation.timeUnit();

        String dynamicKey = parseSpEL(joinPoint, method, keyExpression);
        String lockKey = prefix + ":" + dynamicKey;

        RLock lock = redissonClient.getLock(lockKey);

        boolean isLocked = false;
        try {
            isLocked = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (!isLocked) {
                log.warn("🔒 Lock 획득 실패 - key: {}", lockKey);
                throw new IllegalStateException("현재 작업이 이미 처리 중입니다.");
            }

            log.info("🔐 Lock 획득 성공 - key: {}", lockKey);
            return joinPoint.proceed();
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("🔓 Lock 해제 - key: {}", lockKey);
            }
        }
    }

    private String parseSpEL(ProceedingJoinPoint joinPoint, Method method, String keyExpression) {
        EvaluationContext context = new StandardEvaluationContext();

        Object[] args = joinPoint.getArgs();
        String[] paramNames = nameDiscoverer.getParameterNames(method);
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        Expression expression = parser.parseExpression(keyExpression);
        return expression.getValue(context, String.class);
    }
}
