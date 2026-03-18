package com.xyqb.platform.common.exception;

import com.xyqb.platform.common.enums.BusinessCode;
import com.xyqb.platform.common.enums.SystemCode;

/**
 * 业务异常类
 *
 * <p>用于表示业务逻辑层面的可预期错误，例如：
 * <ul>
 *     <li>请求参数不合法</li>
 *     <li>查询的数据不存在</li>
 *     <li>业务规则校验不通过（如分类名称重复、层级超出限制）</li>
 *     <li>接口限流被触发</li>
 * </ul>
 *
 * <p>业务异常的特点：
 * <ul>
 *     <li>HTTP 状态码返回 200（对前端而言请求本身成功，通过 businessCode 判断结果）</li>
 *     <li>日志级别为 WARN（可预期的、正常的错误分支）</li>
 *     <li>默认系统码为 {@link SystemCode#LOCAL_SERVICE_ERROR}（0002）</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 仅指定业务码（系统码自动为 LOCAL_SERVICE_ERROR）
 * throw new BusinessException(BusinessCode.CATEGORY_NOT_FOUND);
 *
 * // 指定业务码 + 自定义描述
 * throw new BusinessException(BusinessCode.CATEGORY_NOT_FOUND, "父分类不存在: " + parentId);
 *
 * // 同时指定系统码和业务码（如限流场景）
 * throw new BusinessException(SystemCode.RATE_LIMIT_ERROR, BusinessCode.DEFAULT_ERROR);
 * }</pre>
 *
 * @author XYQB Team
 * @since 1.0.0
 * @see BaseException
 * @see GlobalExceptionHandler#handleBusinessException(BusinessException)
 */
public class BusinessException extends BaseException {

    /**
     * 使用业务码构造异常，系统码默认为 LOCAL_SERVICE_ERROR
     *
     * @param businessCode 业务状态码
     */
    public BusinessException(BusinessCode businessCode) {
        super(SystemCode.LOCAL_SERVICE_ERROR, businessCode);
    }

    /**
     * 使用业务码和自定义消息构造异常，系统码默认为 LOCAL_SERVICE_ERROR
     *
     * @param businessCode 业务状态码
     * @param message      自定义异常描述
     */
    public BusinessException(BusinessCode businessCode, String message) {
        super(SystemCode.LOCAL_SERVICE_ERROR, businessCode, message);
    }

    /**
     * 同时指定系统码和业务码
     *
     * @param systemCode   系统状态码
     * @param businessCode 业务状态码
     */
    public BusinessException(SystemCode systemCode, BusinessCode businessCode) {
        super(systemCode, businessCode);
    }
}
