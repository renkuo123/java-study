package com.xyqb.platform.common.filter;

import com.xyqb.platform.common.constant.ApiClientHeaders;
import com.xyqb.platform.common.context.ClientRequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 从 H5 前端常用请求头解析租户与令牌，写入 {@link ClientRequestContext}。
 *
 * <p>令牌优先级：{@code Access-Token} → {@code X-Auth-Token} → {@code Authorization}（Bearer）。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ClientRequestContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String tenantId = request.getHeader(ApiClientHeaders.QG_TENANT_ID);
            String token = resolveToken(request);
            ClientRequestContext.set(tenantId, token);
            filterChain.doFilter(request, response);
        } finally {
            ClientRequestContext.clear();
        }
    }

    private static String resolveToken(HttpServletRequest request) {
        String a = request.getHeader(ApiClientHeaders.ACCESS_TOKEN);
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        String x = request.getHeader(ApiClientHeaders.X_AUTH_TOKEN);
        if (x != null && !x.isBlank()) {
            return x.trim();
        }
        String auth = request.getHeader(ApiClientHeaders.AUTHORIZATION);
        if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String v = auth.substring(7).trim();
            if (!v.isEmpty()) {
                return v;
            }
        }
        return null;
    }
}
