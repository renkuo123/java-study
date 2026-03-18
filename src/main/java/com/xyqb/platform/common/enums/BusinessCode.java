package com.xyqb.platform.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务状态码枚举
 *
 * <p>用于标识具体业务操作的结果，所有接口统一返回结构中的 {@code businessCode} 字段取值于此枚举。
 *
 * <p>编码规则：
 * <ul>
 *     <li>{@code 0000} - 通用成功</li>
 *     <li>{@code 0001} - 通用失败</li>
 *     <li>{@code 1xxx} - 分类管理模块</li>
 *     <li>后续模块可按 {@code 2xxx}, {@code 3xxx} 依次扩展</li>
 * </ul>
 *
 * <p>与 {@link SystemCode} 的区别：SystemCode 标识系统层面的错误类型（如数据库错误、第三方接口错误），
 * BusinessCode 标识具体业务操作的错误原因（如分类不存在、名称重复）。
 *
 * @author XYQB Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum BusinessCode {

    /** 操作成功 */
    SUCCESS("0000", "操作成功"),

    /** 通用操作失败 */
    DEFAULT_ERROR("0001", "操作失败"),

    // ==================== 分类管理模块 1xxx ====================

    /** 查询的分类不存在 */
    CATEGORY_NOT_FOUND("1001", "分类不存在"),

    /** 同一父分类下分类名称已存在 */
    CATEGORY_NAME_DUPLICATE("1002", "分类名称已存在"),

    /** 该分类下存在子分类，不允许直接删除 */
    CATEGORY_HAS_CHILDREN("1003", "该分类下存在子分类，无法删除"),

    /** 分类层级超出系统允许的最大值 */
    CATEGORY_LEVEL_EXCEEDED("1004", "分类层级超出限制");

    /** 业务状态码 */
    private final String code;

    /** 业务状态码描述信息 */
    private final String message;
}
