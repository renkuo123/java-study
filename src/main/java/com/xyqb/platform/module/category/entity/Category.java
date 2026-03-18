package com.xyqb.platform.module.category.entity;

import com.xyqb.platform.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 分类实体类 - MongoDB 文档映射
 *
 * <p>对应 MongoDB 中的 {@code suggestType} 集合。
 * 继承 {@link BaseEntity} 自动获得 {@code id}、{@code createdAt}、{@code updatedAt} 字段。
 *
 * <p>MongoDB 文档示例：
 * <pre>{@code
 * {
 *   "_id": "5f84057e3bbdab73453df68d",
 *   "name": "售前-咨询",
 *   "order": 1,
 *   "createdAt": "2024-01-01T00:00:00",
 *   "updatedAt": "2024-01-01T00:00:00"
 * }
 * }</pre>
 *
 * @author XYQB Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "suggestType")
public class Category extends BaseEntity {

    /**
     * 分类名称
     *
     * <p>{@code @Indexed} 创建 MongoDB 索引，加速按名称查询和去重校验。
     */
    /**
     * 分类展示名（BSON 字段 {@code name}）。
     */
    @Indexed
    @Field("name")
    private String categoryName;

    /**
     * 历史文档可能仅存在此字段（BSON {@code categoryName}），与 {@link #categoryName} 二选一；
     * 新业务写入请只落 {@link #categoryName}，更新时可置空以完成迁移。
     */
    @Field("categoryName")
    private String legacyCategoryName;

    /**
     * 排序权重
     *
     * <p>对应 MongoDB 文档中的 {@code order} 字段，数值越大排序越靠前。
     */
    @Field("order")
    private Integer order;

    /** 名称解析：优先 {@code name}，否则兼容历史 {@code categoryName} */
    public String resolvedDisplayName() {
        if (categoryName != null && !categoryName.isBlank()) {
            return categoryName.trim();
        }
        if (legacyCategoryName != null && !legacyCategoryName.isBlank()) {
            return legacyCategoryName.trim();
        }
        return "";
    }
}
