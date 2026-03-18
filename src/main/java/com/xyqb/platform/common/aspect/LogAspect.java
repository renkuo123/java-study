package com.xyqb.platform.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 接口日志 AOP 切面
 *
 * <p>自动拦截所有 Controller 层方法，记录每次请求的关键信息：
 * <ul>
 *     <li>请求方法（GET/POST/PUT/DELETE）和 URI</li>
 *     <li>调用的 Controller 类名和方法名</li>
 *     <li>请求参数（JSON 序列化）</li>
 *     <li>接口耗时（毫秒）</li>
 *     <li>异常信息（如果发生异常）</li>
 * </ul>
 *
 * <p>日志格式示例：
 * <pre>
 * >>> [GET] /api/v1/categories/tree | CategoryController.getCategoryTree | params: []
 * <<< [GET] /api/v1/categories/tree | CategoryController.getCategoryTree | elapsed: 35ms
 * </pre>
 *
 * <p>切点定义：拦截 {@code com.xyqb.platform.module} 下所有 controller 包中的方法，
 * 新增业务模块的 Controller 会自动纳入日志记录，无需额外配置。
 *
 * @author XYQB Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    /** Jackson 对象映射器，用于将请求参数序列化为 JSON 字符串 */
    private final ObjectMapper objectMapper;

    /**
     * 切点定义：匹配所有业务模块下 controller 包中的所有方法
     *
     * <p>表达式说明：{@code execution(* com.xyqb.platform.module..controller..*(..)))}
     * <ul>
     *     <li>{@code *} - 匹配任意返回类型</li>
     *     <li>{@code com.xyqb.platform.module..} - module 包及其任意深度子包</li>
     *     <li>{@code controller..} - controller 包及其子包</li>
     *     <li>{@code *(..)} - 任意方法名、任意参数列表</li>
     * </ul>
     */
    @Pointcut("execution(* com.xyqb.platform.module..controller..*(..))")
    public void controllerPointcut() {
    }

    /**
     * 环绕通知：在目标方法执行前后分别记录请求和响应日志
     *
     * <p>执行流程：
     * <ol>
     *     <li>记录请求开始时间</li>
     *     <li>从 RequestContextHolder 获取 HTTP 请求信息（方法、URI）</li>
     *     <li>将方法参数序列化为 JSON 并打印入站日志（>>>）</li>
     *     <li>执行目标方法</li>
     *     <li>计算耗时并打印出站日志（<<<）</li>
     *     <li>如果发生异常，打印异常日志后继续向上抛出</li>
     * </ol>
     *
     * @param joinPoint AOP 连接点
     * @return 目标方法的返回值
     * @throws Throwable 目标方法可能抛出的异常（不吞异常，确保全局异常处理器能捕获）
     */
    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 从 Spring 请求上下文中获取当前 HTTP 请求对象
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        String method = "UNKNOWN";
        String uri = "UNKNOWN";
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            method = request.getMethod();
            uri = request.getRequestURI();
        }

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        // 序列化请求参数，序列化失败不影响主流程
        String params;
        try {
            params = objectMapper.writeValueAsString(joinPoint.getArgs());
        } catch (Exception e) {
            params = "unable to serialize";
        }

        log.info(">>> [{}] {} | {}.{} | params: {}", method, uri, className, methodName, params);

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable throwable) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("<<< [{}] {} | {}.{} | elapsed: {}ms | exception: {}",
                    method, uri, className, methodName, elapsed, throwable.getMessage());
            throw throwable;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("<<< [{}] {} | {}.{} | elapsed: {}ms", method, uri, className, methodName, elapsed);

        return result;
    }
}
