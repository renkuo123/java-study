package com.xyqb.platform.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 接口文档配置类
 *
 * <p>基于 SpringDoc OpenAPI 3 自动生成 RESTful API 接口文档，
 * 启动后访问以下地址查看：
 * <ul>
 *     <li>Swagger UI 界面：{@code http://localhost:8080/swagger-ui.html}</li>
 *     <li>OpenAPI JSON：{@code http://localhost:8080/v3/api-docs}</li>
 * </ul>
 *
 * <p>Controller 中通过以下注解丰富文档信息：
 * <ul>
 *     <li>{@code @Tag(name = "模块名")} - 接口分组</li>
 *     <li>{@code @Operation(summary = "接口描述")} - 接口说明</li>
 *     <li>{@code @Parameter(description = "参数描述")} - 参数说明</li>
 * </ul>
 *
 * @author XYQB Team
 * @since 1.0.0
 */
@Configuration
public class SwaggerConfig {

    /**
     * 配置 OpenAPI 文档基本信息
     *
     * @return OpenAPI 配置对象
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("H5 XYQB Platform API")
                        .description(
                        "H5 XYQB 后台管理平台接口文档。\n\n"
                                + "**前端可选请求头（已兼容解析）：** `Access-Token`、`X-Auth-Token`（令牌，二者取一即可）、"
                                + "`qg-tenant-id`（租户）、`Authorization: Bearer <token>`。\n"
                                + "上下文见 `ClientRequestContext`，当前不强制鉴权。")
                        .version("v1.0.0")
                        .contact(new Contact().name("XYQB Team")));
    }
}
