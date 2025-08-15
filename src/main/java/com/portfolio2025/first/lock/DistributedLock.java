package com.portfolio2025.first.lock;


import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface DistributedLock {
    String prefix();                  // 락 키 prefix
    String key();                     // EL 형식 키 ex) "#dto.stockOrderId"
    long waitTime() default 3L;       // 락 대기 시간
    long leaseTime() default 10L;     // 락 유지 시간
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}

