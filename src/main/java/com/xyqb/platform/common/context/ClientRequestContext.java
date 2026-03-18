package com.xyqb.platform.common.context;

import java.util.Optional;

/**
 * 当前请求解析后的前端上下文（租户、令牌等），基于 ThreadLocal，请求结束务必清理。
 */
public final class ClientRequestContext {

    private static final ThreadLocal<Context> HOLDER = new ThreadLocal<>();

    private ClientRequestContext() {}

    public static void set(String tenantId, String accessToken) {
        HOLDER.set(new Context(
                tenantId != null && !tenantId.isBlank() ? tenantId.trim() : null,
                accessToken != null && !accessToken.isBlank() ? accessToken.trim() : null));
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static Optional<String> getTenantId() {
        Context c = HOLDER.get();
        return c != null && c.tenantId != null ? Optional.of(c.tenantId) : Optional.empty();
    }

    public static Optional<String> getAccessToken() {
        Context c = HOLDER.get();
        return c != null && c.accessToken != null ? Optional.of(c.accessToken) : Optional.empty();
    }

    private record Context(String tenantId, String accessToken) {}
}
