package com.xyqb.platform.common.exception;

import com.xyqb.platform.common.enums.BusinessCode;
import com.xyqb.platform.common.enums.SystemCode;
import com.xyqb.platform.common.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * <p>基于 {@code @RestControllerAdvice} 实现全局统一异常处理，
 * 拦截所有 Controller 层抛出的异常，将其转换为标准的 {@link ApiResponse} 格式返回。
 *
 * <p>核心设计原则：
 * <ul>
 *     <li>对外统一格式：无论什么异常，客户端收到的都是 {@code {businessCode, code, data}} 结构</li>
 *     <li>对内保留细节：通过日志记录详细的异常信息和堆栈，便于排查问题</li>
 *     <li>防止信息泄露：堆栈信息绝不返回给客户端，避免安全风险</li>
 * </ul>
 *
 * <p>异常处理优先级（从高到低）：
 * <ol>
 *     <li>自定义业务异常 {@link BusinessException} → HTTP 200 + 错误码</li>
 *     <li>自定义系统异常 {@link SystemException} → HTTP 500</li>
 *     <li>参数校验异常 → HTTP 400</li>
 *     <li>数据库访问异常 → HTTP 500</li>
 *     <li>兜底处理 {@link Exception} → HTTP 500</li>
 * </ol>
 *
 * @author XYQB Team
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     *
     * <p>业务异常是可预期的错误（如数据不存在、参数不合法），
     * HTTP 状态码返回 200，通过 code 和 businessCode 让前端判断结果。
     * 日志级别为 WARN，表示这是正常的错误分支。
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: systemCode={}, businessCode={}, message={}",
                e.getSystemCode().getCode(), e.getBusinessCode().getCode(), e.getMessage());
        return ApiResponse.fail(e.getSystemCode(), e.getBusinessCode());
    }

    /**
     * 处理系统异常
     *
     * <p>系统异常是非预期的严重错误（如数据库连接断开），
     * HTTP 状态码返回 500，日志级别为 ERROR 并打印完整堆栈。
     */
    @ExceptionHandler(SystemException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleSystemException(SystemException e) {
        log.error("系统异常: systemCode={}, message={}",
                e.getSystemCode().getCode(), e.getMessage(), e);
        return ApiResponse.fail(e.getSystemCode(), e.getBusinessCode());
    }

    /**
     * 处理 @RequestBody 参数校验异常
     *
     * <p>当 Controller 方法参数标注了 {@code @Valid}，且请求体中的字段不满足
     * {@code @NotBlank}, {@code @Size} 等校验注解时触发。
     * 提取所有校验失败的字段信息，拼接后记录日志。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", errors);
        return ApiResponse.fail(SystemCode.PARAM_VALIDATION_ERROR, BusinessCode.DEFAULT_ERROR);
    }

    /**
     * 处理 @PathVariable / @RequestParam 参数校验异常
     *
     * <p>当 Controller 类上标注了 {@code @Validated}，
     * 且路径参数或请求参数不满足约束条件时触发。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException e) {
        String errors = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("约束违反: {}", errors);
        return ApiResponse.fail(SystemCode.PARAM_VALIDATION_ERROR, BusinessCode.DEFAULT_ERROR);
    }

    /**
     * 处理必填请求参数缺失异常
     *
     * <p>当 {@code @RequestParam(required=true)} 的参数未传递时触发。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return ApiResponse.fail(SystemCode.PARAM_VALIDATION_ERROR, BusinessCode.DEFAULT_ERROR);
    }

    /**
     * 处理请求体无法解析异常
     *
     * <p>当请求体的 JSON 格式不合法或无法反序列化为目标类型时触发。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体无法解析: {}", e.getMessage());
        return ApiResponse.fail(SystemCode.PARAM_VALIDATION_ERROR, BusinessCode.DEFAULT_ERROR);
    }

    /**
     * 处理请求方法不支持异常
     *
     * <p>例如接口只允许 GET，但客户端发送了 POST 请求。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<Void> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不允许: {}", e.getMethod());
        return ApiResponse.fail(SystemCode.METHOD_NOT_ALLOWED, BusinessCode.DEFAULT_ERROR);
    }

    /**
     * 处理资源不存在异常
     *
     * <p>当请求的静态资源或路由不存在时触发。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("资源不存在: {}", e.getResourcePath());
        return ApiResponse.fail(SystemCode.NOT_FOUND, BusinessCode.DEFAULT_ERROR);
    }

    /**
     * 处理数据库访问异常
     *
     * <p>{@link DataAccessException} 是 Spring 对所有数据库异常的统一抽象，
     * 包括 MongoDB 的连接失败、查询超时、写入冲突等。
     * 日志级别为 ERROR，需要运维关注。
     */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleDataAccessException(DataAccessException e) {
        log.error("数据库访问异常", e);
        return ApiResponse.fail(SystemCode.MONGO_ERROR, BusinessCode.DEFAULT_ERROR);
    }

    /**
     * 兜底异常处理
     *
     * <p>捕获所有未被上述处理器匹配到的异常，确保任何情况下客户端都能收到标准格式响应。
     * 日志级别为 ERROR，需要开发排查。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleUnknownException(Exception e) {
        log.error("未知异常", e);
        return ApiResponse.fail(SystemCode.DEFAULT_ERROR, BusinessCode.DEFAULT_ERROR);
    }
}
