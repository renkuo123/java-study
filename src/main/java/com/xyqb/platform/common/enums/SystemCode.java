package com.xyqb.platform.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统状态码枚举
 *
 * <p>用于标识系统层面的错误类型，所有接口统一返回结构中的 {@code code} 字段取值于此枚举。
 *
 * <p>状态码规则：
 * <ul>
 *     <li>{@code 0000} - 成功</li>
 *     <li>{@code 0001} - 默认错误（未归类的通用错误）</li>
 *     <li>{@code 0002} - 本地服务错误（参数校验、限流、资源不存在等）</li>
 *     <li>{@code 0003} - HTTP 请求错误（调用外部 HTTP 接口失败）</li>
 *     <li>{@code 0004} - MongoDB 数据库错误</li>
 *     <li>{@code 0005} - Redis 缓存错误</li>
 *     <li>{@code 0006} - 第三方接口错误</li>
 * </ul>
 *
 * <p>设计说明：参数校验、限流、资源不存在等本地服务问题统一归类为 {@code 0002}，
 * 通过 {@code businessCode} 做更细粒度的区分。
 *
 * @author XYQB Team
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum SystemCode {

    /** 请求处理成功 */
    SUCCESS("0000", "成功"),

    /** 未归类的通用错误 */
    DEFAULT_ERROR("0001", "默认错误"),

    /** 本地服务错误（业务逻辑层面） */
    LOCAL_SERVICE_ERROR("0002", "本地服务错误"),

    /** 调用外部 HTTP 接口失败 */
    HTTP_REQUEST_ERROR("0003", "HTTP请求错误"),

    /** MongoDB 数据库操作错误 */
    MONGO_ERROR("0004", "MongoDB错误"),

    /** Redis 缓存操作错误 */
    REDIS_ERROR("0005", "Redis错误"),

    /** 第三方接口调用错误 */
    THIRD_PARTY_ERROR("0006", "第三方接口错误"),

    /** 请求参数校验失败（归属本地服务错误 0002） */
    PARAM_VALIDATION_ERROR("0002", "参数校验失败"),

    /** 接口限流触发（归属本地服务错误 0002） */
    RATE_LIMIT_ERROR("0002", "请求过于频繁"),

    /** 请求的资源不存在（归属本地服务错误 0002） */
    NOT_FOUND("0002", "资源不存在"),

    /** 请求方法不被允许（归属本地服务错误 0002） */
    METHOD_NOT_ALLOWED("0002", "请求方法不允许");

    /** 状态码 */
    private final String code;

    /** 状态码描述信息 */
    private final String message;
}
