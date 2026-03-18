package com.xyqb.platform.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 *
 * <p>标注在 Controller 方法上，通过 AOP 切面 {@link com.xyqb.platform.common.aspect.RateLimitAspect}
 * 自动拦截，基于 Guava {@code RateLimiter} 令牌桶算法实现单机限流。
 *
 * <p>工作原理：
 * <ul>
 *     <li>每个被标注的方法会独立持有一个 RateLimiter 实例</li>
 *     <li>RateLimiter 按 {@code permitsPerSecond} 速率持续产生令牌</li>
 *     <li>请求到达时尝试获取令牌，获取成功则放行，失败则抛出限流异常</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * @GetMapping("/list")
 * @RateLimit(permitsPerSecond = 20)  // 每秒最多 20 次请求
 * public ApiResponse<List<CategoryVO>> list() { ... }
 *
 * @PostMapping
 * @RateLimit(permitsPerSecond = 5)   // 写操作限制更严格
 * public ApiResponse<CategoryVO> create(@RequestBody CategoryCreateDTO dto) { ... }
 * }</pre>
 *
 * @author XYQB Team
 * @since 1.0.0
 * @see com.xyqb.platform.common.aspect.RateLimitAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 每秒允许的最大请求数
     *
     * <p>默认值 10.0，即每秒最多 10 次请求。
     * 建议读接口设置较高（如 20），写接口设置较低（如 5）。
     *
     * @return 每秒允许的请求数
     */
    double permitsPerSecond() default 10.0;
}
