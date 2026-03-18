package com.xyqb.platform.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB 配置类
 *
 * <p>负责 MongoDB 相关的 Spring Data 配置：
 * <ul>
 *     <li>{@code @EnableMongoAuditing} - 启用审计功能，使 {@code @CreatedDate} 和
 *         {@code @LastModifiedDate} 注解生效，自动填充实体的创建时间和修改时间</li>
 *     <li>{@code @EnableMongoRepositories} - 指定 Repository 接口的扫描路径，
 *         Spring Data 会自动为这些接口生成实现类并注册到容器中</li>
 * </ul>
 *
 * <p>MongoDB 连接信息通过 {@code application-{profile}.yml} 中的
 * {@code spring.data.mongodb.uri} 配置，支持多环境切换。
 *
 * @author XYQB Team
 * @since 1.0.0
 */
@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "com.xyqb.platform.module")
public class MongoConfig {
}
