# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在本仓库中工作时提供指引。

## 构建与运行

```bash
# 构建（含测试）
./mvnw clean package

# 构建（跳过测试）
./mvnw clean package -DskipTests

# 运行测试
./mvnw test

# 运行单个测试类
./mvnw test -Dtest=CategoryServiceTest

# 运行单个测试方法
./mvnw test -Dtest=CategoryServiceTest#shouldReturnCategoryById

# 本地启动（默认 dev 配置）
java -jar target/h5-xyqb-platform-1.0.0-SNAPSHOT.jar

# 生产启动
java -jar -Dspring.profiles.active=prod target/h5-xyqb-platform-*.jar
```

**本地访问：** API `http://localhost:8080/api/v1/...`，Swagger UI `/swagger-ui.html`，健康检查 `/actuator/health`。

## 技术栈

- Java 17，Spring Boot 3.2.5，Maven
- **仅使用 MongoDB**（Spring Data MongoDB，无 JPA/MyBatis/SQL）
- **Redis 缓存**（Spring Data Redis + Lettuce，手动 Cache-Aside 模式）
- SpringDoc OpenAPI 生成 API 文档
- Jakarta Validation、Lombok、Guava（限流）
- 测试：JUnit 5 + Mockito + AssertJ

## 架构

业务模块按垂直切片组织在 `com.xyqb.platform.module.{模块名}` 下，公共横切代码在 `com.xyqb.platform.common`。

**模块包结构**（新模块以 `category` 为模板）：
```
module/{模块名}/
├── controller/      # @RestController，委托 Service，返回 ApiResponse
├── service/         # 接口
├── service/impl/    # 实现，包含业务逻辑
├── entity/          # MongoDB @Document，继承 BaseEntity
├── dto/             # 入参 DTO，带校验注解
├── vo/              # 出参视图对象
└── repository/      # MongoRepository<Entity, String>
```

**核心横切类：**
- `ApiResponse<T>` — 统一响应包装（`success`/`fail`），所有 Controller 必须返回此类型
- `BusinessException` / `SystemException` — 继承 `BaseException`；业务错误返回 HTTP 200 + 错误码，系统错误返回 HTTP 500
- `BusinessCode` / `SystemCode` — 枚举错误码；新增业务错误时扩展 `BusinessCode`
- `BaseEntity` — MongoDB 基类，含 `id`、`createdAt`、`updatedAt`（自动审计）
- `PageResult<T>` — 通用分页 VO
- `@RateLimit` — 方法级限流注解（读接口 20 QPS，写接口 5 QPS）
- `ClientRequestContext` — ThreadLocal，存放请求头中的租户 ID 和访问令牌
- `GlobalExceptionHandler` — 将异常映射为 `ApiResponse`
- `RedisConfig` — 配置 RedisTemplate 的 JSON 序列化
- `RedisCacheService` — 通用缓存工具类（set/get/getList/delete/hasKey），读操作降级

## 编码约定

- API 路径前缀：`AppConstants.API_PREFIX` = `/api/v1`
- Controller 不写业务逻辑，只委托 Service 并用 `ApiResponse.success()` 包装
- Mongo 集合名可与 Java 类名不同（如 `Category` 实体对应集合 `suggestType`）；BSON 字段名与 Java 不同时用 `@Field("mongoFieldName")`
- 可预期业务错误：抛 `BusinessException`（HTTP 200 + 错误码）；参数校验失败：HTTP 400，由 `GlobalExceptionHandler` 处理
- 所有公开接口需加 `@Operation`/`@Tag` Swagger 注解和 `@RateLimit`

## 测试约定

- **Controller 测试：** `@WebMvcTest(XxxController.class)` + `@MockBean` Service + `MockMvc` + `jsonPath` 断言 `code`/`businessCode`/`data`
- **Service 测试：** `@ExtendWith(MockitoExtension.class)` + `@Mock` Repository + `@InjectMocks` Service 实现 + `@Nested` 按方法分组 + AssertJ + BDD 风格（given/when/then）
- 新接口 = 新增 `*ControllerTest`；新业务逻辑 = 新增或扩展 `*ServiceTest`

## 文档同步规则

变更技术栈（pom.xml 依赖、基础设施、全局横切能力）时，必须在**同一批改动**中同步更新：
1. `docs/ai-index.md` — 仓库概览与横切速查
2. `.cursor/rules/*.mdc` — `project-core.mdc` 技术栈摘要；涉及 Java/测试约定则改 `java-backend.mdc`/`testing.mdc`
