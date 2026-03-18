package com.xyqb.platform.common.base;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

/**
 * MongoDB 文档实体基类
 *
 * <p>所有 MongoDB 文档实体都应继承此基类，自动获得以下公共字段：
 * <ul>
 *     <li>{@code id} - MongoDB 文档的唯一标识（对应 _id 字段），类型为 String（MongoDB ObjectId）</li>
 *     <li>{@code createdAt} - 文档创建时间，由 Spring Data MongoDB 审计机制自动填充</li>
 *     <li>{@code updatedAt} - 文档最后修改时间，每次 save 操作时自动更新</li>
 * </ul>
 *
 * <p>审计功能依赖 {@link com.xyqb.platform.common.config.MongoConfig} 中的
 * {@code @EnableMongoAuditing} 注解启用。
 *
 * <p>使用示例：
 * <pre>{@code
 * @Document(collection = "categories")
 * public class Category extends BaseEntity {
 *     private String categoryName;
 *     private Integer level;
 * }
 * }</pre>
 *
 * @author XYQB Team
 * @since 1.0.0
 */
@Data
public abstract class BaseEntity {

    /**
     * MongoDB 文档唯一标识
     *
     * <p>{@code @Id} 注解将此字段映射到 MongoDB 的 {@code _id} 字段。
     * 如果保存时未设置值，MongoDB 会自动生成 24 位的 ObjectId 字符串。
     */
    @Id
    private String id;

    /**
     * 文档创建时间
     *
     * <p>{@code @CreatedDate} 由 Spring Data 审计机制在首次 save 时自动填充，
     * 后续更新操作不会修改此字段。
     */
    @CreatedDate
    private LocalDateTime createdAt;

    /**
     * 文档最后修改时间
     *
     * <p>{@code @LastModifiedDate} 由 Spring Data 审计机制在每次 save 时自动更新。
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
