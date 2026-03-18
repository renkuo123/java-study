package com.xyqb.platform.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xyqb.platform.common.enums.BusinessCode;
import com.xyqb.platform.common.enums.SystemCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应封装类
 *
 * <p>所有接口的返回值都通过此类进行包装，确保前后端交互的数据格式一致。
 *
 * <p>返回格式示例：
 * <pre>{@code
 * {
 *   "businessCode": "0000",
 *   "code": "0000",
 *   "data": { ... }
 * }
 * }</pre>
 *
 * <p>使用方式：
 * <ul>
 *     <li>成功响应：{@code ApiResponse.success(data)}</li>
 *     <li>无数据成功响应：{@code ApiResponse.success()}</li>
 *     <li>失败响应：{@code ApiResponse.fail(SystemCode.XXX, BusinessCode.XXX)}</li>
 * </ul>
 *
 * @param <T> 响应数据的泛型类型
 * @author XYQB Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /** 业务状态码，标识具体业务操作的结果 */
    private String businessCode;

    /** 系统状态码，标识系统层面的错误类型 */
    private String code;

    /** 响应数据，成功时返回具体数据，失败时通常为 null */
    private T data;

    /**
     * 提示信息
     *
     * <p>约定：
     * <ul>
     *     <li>成功时：取 {@link BusinessCode#SUCCESS} 的描述</li>
     *     <li>失败时：优先取 {@link BusinessCode} 的描述，若无则回退为 {@link SystemCode} 的描述</li>
     * </ul>
     */
    private String message;

    /**
     * 构建成功响应（带数据）
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 包含 code=0000, businessCode=0000 的成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .businessCode(BusinessCode.SUCCESS.getCode())
                .code(SystemCode.SUCCESS.getCode())
                .message(BusinessCode.SUCCESS.getMessage())
                .data(data)
                .build();
    }

    /**
     * 构建成功响应（无数据）
     *
     * <p>适用于删除、更新等不需要返回数据的操作。
     *
     * @param <T> 数据类型
     * @return 包含 code=0000, businessCode=0000, data=null 的成功响应
     */
    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    /**
     * 构建失败响应（使用枚举）
     *
     * @param systemCode   系统状态码枚举
     * @param businessCode 业务状态码枚举
     * @param <T>          数据类型
     * @return 失败响应，data 为 null
     */
    public static <T> ApiResponse<T> fail(SystemCode systemCode, BusinessCode businessCode) {
        return ApiResponse.<T>builder()
                .code(systemCode.getCode())
                .businessCode(businessCode.getCode())
                .message(businessCode.getMessage())
                .build();
    }

    /**
     * 构建失败响应（系统码枚举 + 业务码字符串）
     *
     * <p>适用于业务码需要动态拼接的场景。
     *
     * @param systemCode   系统状态码枚举
     * @param businessCode 业务状态码字符串
     * @param <T>          数据类型
     * @return 失败响应
     */
    public static <T> ApiResponse<T> fail(SystemCode systemCode, String businessCode) {
        return ApiResponse.<T>builder()
                .code(systemCode.getCode())
                .businessCode(businessCode)
                .message(systemCode.getMessage())
                .build();
    }

    /**
     * 构建失败响应（双字符串）
     *
     * <p>适用于完全自定义状态码的场景。
     *
     * @param systemCode   系统状态码字符串
     * @param businessCode 业务状态码字符串
     * @param <T>          数据类型
     * @return 失败响应
     */
    public static <T> ApiResponse<T> fail(String systemCode, String businessCode) {
        return ApiResponse.<T>builder()
                .code(systemCode)
                .businessCode(businessCode)
                .build();
    }
}
