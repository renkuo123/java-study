package com.xyqb.platform.common.exception;

import com.xyqb.platform.common.enums.BusinessCode;
import com.xyqb.platform.common.enums.SystemCode;

/**
 * 系统异常类
 *
 * <p>用于表示系统层面的不可预期错误，例如：
 * <ul>
 *     <li>MongoDB 连接失败或查询超时</li>
 *     <li>Redis 连接异常</li>
 *     <li>第三方 HTTP 接口调用失败</li>
 *     <li>其他基础设施层的非预期错误</li>
 * </ul>
 *
 * <p>系统异常的特点：
 * <ul>
 *     <li>HTTP 状态码返回 500</li>
 *     <li>日志级别为 ERROR（需要关注和排查的严重错误）</li>
 *     <li>通常携带原始异常 cause，便于定位根因</li>
 *     <li>业务码默认为 {@link BusinessCode#DEFAULT_ERROR}</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 捕获数据库异常后包装
 * try {
 *     repository.save(entity);
 * } catch (DataAccessException e) {
 *     throw new SystemException(SystemCode.MONGO_ERROR, e);
 * }
 *
 * // 捕获第三方接口异常
 * try {
 *     httpClient.call(url);
 * } catch (Exception e) {
 *     throw new SystemException(SystemCode.THIRD_PARTY_ERROR, BusinessCode.DEFAULT_ERROR, e);
 * }
 * }</pre>
 *
 * @author XYQB Team
 * @since 1.0.0
 * @see BaseException
 * @see GlobalExceptionHandler#handleSystemException(SystemException)
 */
public class SystemException extends BaseException {

    /**
     * 使用系统码构造异常，业务码默认为 DEFAULT_ERROR
     *
     * @param systemCode 系统状态码
     */
    public SystemException(SystemCode systemCode) {
        super(systemCode, BusinessCode.DEFAULT_ERROR);
    }

    /**
     * 使用系统码和原始异常构造，业务码默认为 DEFAULT_ERROR
     *
     * <p>适用于捕获底层异常后包装的场景，保留原始堆栈。
     *
     * @param systemCode 系统状态码
     * @param cause      原始异常
     */
    public SystemException(SystemCode systemCode, Throwable cause) {
        super(systemCode, BusinessCode.DEFAULT_ERROR, cause);
    }

    /**
     * 同时指定系统码、业务码和原始异常
     *
     * @param systemCode   系统状态码
     * @param businessCode 业务状态码
     * @param cause        原始异常
     */
    public SystemException(SystemCode systemCode, BusinessCode businessCode, Throwable cause) {
        super(systemCode, businessCode, cause);
    }
}
