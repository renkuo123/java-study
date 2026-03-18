package com.xyqb.platform.common.aspect;

import com.google.common.util.concurrent.RateLimiter;
import com.xyqb.platform.common.annotation.RateLimit;
import com.xyqb.platform.common.enums.BusinessCode;
import com.xyqb.platform.common.enums.SystemCode;
import com.xyqb.platform.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 接口限流 AOP 切面
 *
 * <p>拦截所有标注了 {@link RateLimit} 注解的方法，通过 Guava {@link RateLimiter}
 * 令牌桶算法实现单机接口限流。
 *
 * <p>实现细节：
 * <ul>
 *     <li>使用 {@link ConcurrentHashMap} 缓存每个方法对应的 RateLimiter 实例，key 为「类名#方法名」</li>
 *     <li>{@code computeIfAbsent} 保证并发场景下每个方法只创建一个 RateLimiter（线程安全）</li>
 *     <li>{@code tryAcquire()} 非阻塞获取令牌：获取成功放行，失败立即拒绝（不等待）</li>
 * </ul>
 *
 * <p>局限性说明：当前为单机限流，分布式部署时需替换为 Redis + Lua 方案。
 *
 * @author XYQB Team
 * @since 1.0.0
 * @see RateLimit
 */
@Slf4j
@Aspect
@Component
@SuppressWarnings("UnstableApiUsage")
public class RateLimitAspect {

    /** 方法级别的 RateLimiter 缓存，key 为「全限定类名#方法名」 */
    private final ConcurrentHashMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    /**
     * 环绕通知：在目标方法执行前进行限流判断
     *
     * <p>执行流程：
     * <ol>
     *     <li>通过反射获取目标方法上的 @RateLimit 注解</li>
     *     <li>根据「类名#方法名」查找或创建对应的 RateLimiter</li>
     *     <li>尝试非阻塞获取令牌</li>
     *     <li>获取成功 → 执行目标方法；获取失败 → 抛出 BusinessException</li>
     * </ol>
     *
     * @param joinPoint AOP 连接点，包含目标方法的所有信息
     * @return 目标方法的返回值
     * @throws Throwable 目标方法抛出的异常或限流异常
     */
    @Around("@annotation(com.xyqb.platform.common.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        String key = method.getDeclaringClass().getName() + "#" + method.getName();
        RateLimiter limiter = limiters.computeIfAbsent(key,
                k -> RateLimiter.create(rateLimit.permitsPerSecond()));

        if (!limiter.tryAcquire()) {
            log.warn("接口限流触发: {}", key);
            throw new BusinessException(SystemCode.RATE_LIMIT_ERROR, BusinessCode.DEFAULT_ERROR);
        }

        return joinPoint.proceed();
    }
}
