package com.xyqb.platform.module.category.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分类视图对象（View Object）
 *
 * <p>用于向客户端返回分类数据，与 Entity 层解耦。
 * VO 只包含前端需要展示的字段，隐藏内部实现细节（如 createdAt、updatedAt）。
 *
 * <p>{@code @JsonInclude(NON_NULL)} 确保 null 字段不会出现在 JSON 响应中，
 * 例如叶子节点的 {@code children} 为 null 时不返回该字段，保持响应简洁。
 *
 * <p>树形结构响应示例：
 * <pre>{@code
 * {
 * "id": "5f84057e3bbdab73453df68d",
 * "name": "商城购物",
 * "order": 1
 * }
 * }</pre>
 *
 * <p>VO 与 DTO 的区别：
 * <ul>
 * <li>DTO 用于接收客户端输入（入参），VO 用于返回给客户端（出参）</li>
 * <li>DTO 上有校验注解，VO 上有序列化控制注解</li>
 * </ul>
 *
 * @author XYQB Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryVO {

    /** 分类 ID */
    private String id;

    /** 分类名称 */
    private String name;

    /** 排序权重，数值越大排序越靠前 */
    private Integer order;
}
