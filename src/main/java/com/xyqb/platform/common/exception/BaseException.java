package com.xyqb.platform.common.exception;

import com.xyqb.platform.common.enums.BusinessCode;
import com.xyqb.platform.common.enums.SystemCode;
import lombok.Getter;

/**
 * 自定义异常基类（抽象类）
 *
 * <p>所有业务异常和系统异常都必须继承此基类，确保异常体系统一。
 * 每个异常都携带两个核心信息：
 * <ul>
 *     <li>{@code systemCode} - 系统状态码，标识错误发生在哪个层面（数据库、第三方、本地服务等）</li>
 *     <li>{@code businessCode} - 业务状态码，标识具体的业务错误原因</li>
 * </ul>
 *
 * <p>继承关系：
 * <pre>
 * BaseException (抽象基类)
 *   ├── BusinessException  (业务逻辑异常：参数错误、数据不存在、业务规则冲突等)
 *   └── SystemException    (系统级异常：数据库连接失败、第三方调用超时等)
 * </pre>
 *
 * <p>继承 {@link RuntimeException} 而非 {@link Exception} 的原因：
 * 业务异常属于非受检异常，不应强制调用方 try-catch，
 * 统一由 {@link GlobalExceptionHandler} 全局捕获处理。
 *
 * @author XYQB Team
 * @since 1.0.0
 */
@Getter
public abstract class BaseException extends RuntimeException {

    /** 系统状态码 */
    private final SystemCode systemCode;

    /** 业务状态码 */
    private final BusinessCode businessCode;

    /**
     * 使用系统码和业务码构造异常，异常信息取自业务码的描述
     *
     * @param systemCode   系统状态码
     * @param businessCode 业务状态码
     */
    protected BaseException(SystemCode systemCode, BusinessCode businessCode) {
        super(businessCode.getMessage());
        this.systemCode = systemCode;
        this.businessCode = businessCode;
    }

    /**
     * 使用系统码、业务码和自定义消息构造异常
     *
     * <p>适用于需要在业务码描述基础上补充更多上下文信息的场景，
     * 例如：{@code "父分类不存在: 5f84057e3bbdab73453df68d"}
     *
     * @param systemCode   系统状态码
     * @param businessCode 业务状态码
     * @param message      自定义异常描述信息
     */
    protected BaseException(SystemCode systemCode, BusinessCode businessCode, String message) {
        super(message);
        this.systemCode = systemCode;
        this.businessCode = businessCode;
    }

    /**
     * 使用系统码、业务码和原始异常构造异常
     *
     * <p>适用于捕获底层异常后包装为业务异常的场景，保留原始异常堆栈便于排查。
     *
     * @param systemCode   系统状态码
     * @param businessCode 业务状态码
     * @param cause        原始异常
     */
    protected BaseException(SystemCode systemCode, BusinessCode businessCode, Throwable cause) {
        super(businessCode.getMessage(), cause);
        this.systemCode = systemCode;
        this.businessCode = businessCode;
    }
}
