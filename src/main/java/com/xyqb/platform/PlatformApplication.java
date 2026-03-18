package com.xyqb.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * H5 XYQB 后台管理平台 - 应用启动类
 *
 * <p>{@code @SpringBootApplication} 是一个组合注解，等价于同时使用：
 * <ul>
 *     <li>{@code @Configuration} - 标识当前类为配置类，允许通过 @Bean 注册组件</li>
 *     <li>{@code @EnableAutoConfiguration} - 启用 Spring Boot 自动配置机制，根据 classpath 下的依赖自动配置 Bean</li>
 *     <li>{@code @ComponentScan} - 自动扫描当前包及子包下的所有 Spring 组件（@Component, @Service, @Controller 等）</li>
 * </ul>
 *
 * <p>Spring Boot 会自动扫描 {@code com.xyqb.platform} 及其所有子包，
 * 因此 common 和 module 下的所有组件都会被自动注册到 Spring 容器中。
 *
 * @author XYQB Team
 * @since 1.0.0
 */
@SpringBootApplication
public class PlatformApplication {

    /**
     * 应用入口方法
     *
     * <p>{@code SpringApplication.run()} 会依次完成以下工作：
     * <ol>
     *     <li>创建 ApplicationContext（Spring 容器）</li>
     *     <li>加载所有自动配置和用户定义的 Bean</li>
     *     <li>启动内嵌的 Tomcat 服务器</li>
     *     <li>开始监听 HTTP 请求</li>
     * </ol>
     *
     * @param args 命令行参数，可通过 {@code --server.port=9090} 等形式覆盖配置
     */
    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }
}
