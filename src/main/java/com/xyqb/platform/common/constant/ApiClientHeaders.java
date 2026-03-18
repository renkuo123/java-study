package com.xyqb.platform.common.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * H5 前端常用请求头名称（与浏览器实际发送保持一致，大小写敏感）。
 *
 * <p>服务端会识别这些头并写入 {@link com.xyqb.platform.common.context.ClientRequestContext}，
 * 供后续鉴权、多租户等扩展使用；当前仅解析与透传兼容，不强制校验。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiClientHeaders {

    /** 与 {@link #X_AUTH_TOKEN} 二选一或同时存在，取非空值 */
    public static final String ACCESS_TOKEN = "Access-Token";

    public static final String X_AUTH_TOKEN = "X-Auth-Token";

    /** 租户 ID（字符串，便于与大整数对齐） */
    public static final String QG_TENANT_ID = "qg-tenant-id";

    public static final String AUTHORIZATION = "Authorization";
}
