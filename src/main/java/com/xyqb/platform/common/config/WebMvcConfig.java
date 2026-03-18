package com.xyqb.platform.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置类
 *
 * <p>通过实现 {@link WebMvcConfigurer} 接口自定义 Spring MVC 的行为。
 * 当前主要配置全局 CORS（跨域资源共享）策略。
 *
 * <p>CORS 配置说明：
 * <ul>
 *     <li>{@code addMapping("/**")} - 所有接口路径都允许跨域</li>
 *     <li>{@code allowedOriginPatterns("*")} - 允许来自任意域名的请求（生产环境建议限制为具体域名）</li>
 *     <li>{@code allowedMethods} - 允许的 HTTP 方法</li>
 *     <li>{@code allowCredentials(true)} - 允许携带 Cookie</li>
 *     <li>{@code maxAge(3600)} - 预检请求（OPTIONS）结果缓存 1 小时，减少预检请求次数</li>
 * </ul>
 *
 * @author XYQB Team
 * @since 1.0.0
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 配置全局 CORS 跨域策略
     *
     * @param registry CORS 注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
