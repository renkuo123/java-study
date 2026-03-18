# AI / 开发者索引 — h5-xyqb-platform

> 与 `.cursor/rules/*.mdc` 配合使用：规则写「怎么做」，本文写「在哪、有哪些模块」。

## 维护约定（技术栈 / 架构变更）

**凡改动技术栈或全局能力，须在同一 MR 内同步更新：**

1. **`docs/ai-index.md`** — 更新上表「仓库概览」、「横切文件速查」；新增中间件则补一节或 checklist。
2. **`.cursor/rules/*.mdc`** — 更新 `project-core.mdc` 中的技术栈与横切说明；Java/测试约定变化则改 `java-backend.mdc` / `testing.mdc`；大块新领域可新增专用 `.mdc`（设好 `globs` 或 `alwaysApply`）。

**触发示例：** 改 `pom.xml`、换数据库或 ORM、加 Redis/MQ、换 API 文档方案、改统一异常/返回体策略。

团队评审/MR 模板可加勾选项：*技术栈或基础设施有变 → 已更新 ai-index 与 rules*。

## 仓库概览

| 项 | 说明 |
|----|------|
| 构建 | Maven，`pom.xml`，Java 17 |
| 入口 | `com.xyqb.platform.PlatformApplication` |
| 配置 | `src/main/resources/application.yml`（profile: dev/prod） |
| 日志 | `src/main/resources/logback-spring.xml` |

## 目录结构（主代码）

```
src/main/java/com/xyqb/platform/
├── PlatformApplication.java
├── common/                    # 横切
│   ├── annotation/            # e.g. RateLimit
│   ├── aspect/                # LogAspect, RateLimitAspect
│   ├── base/BaseEntity.java   # Mongo 文档基类 + 审计字段
│   ├── config/                # SwaggerConfig, WebMvcConfig, MongoConfig
│   ├── constant/AppConstants.java, ApiClientHeaders.java（H5 请求头名）
│   ├── context/ClientRequestContext.java（租户/令牌，ThreadLocal）
│   ├── filter/ClientRequestContextFilter.java
│   ├── enums/BusinessCode.java, SystemCode.java
│   ├── exception/             # Base/Business/System + GlobalExceptionHandler
│   ├── response/ApiResponse.java
│   └── vo/PageResult.java     # 通用分页（list、total、pageNo、pageSize、totalPages）
└── module/
    └── category/              # 当前唯一业务模块（模板）
        ├── controller/
        ├── service/, service/impl/
        ├── entity/
        ├── dto/
        ├── vo/
        └── repository/
```

## 目录结构（测试）

```
src/test/java/com/xyqb/platform/module/category/
├── controller/CategoryControllerTest.java
└── service/CategoryServiceTest.java
```

## 横切文件速查

| 用途 | 路径 |
|------|------|
| 统一返回体 | `common/response/ApiResponse.java` |
| 全局异常 | `common/exception/GlobalExceptionHandler.java` |
| 业务/系统异常 | `BusinessException`, `SystemException` |
| 状态码枚举 | `common/enums/BusinessCode.java`, `SystemCode.java` |
| API 前缀 | `common/constant/AppConstants.java` → `/api/v1` |
| Mongo 审计 | `common/config/MongoConfig.java` + `BaseEntity` |
| Swagger | `common/config/SwaggerConfig.java` |
| 限流 | `@RateLimit` + `RateLimitAspect` |
| 分页 VO | `common/vo/PageResult.java` |

## 模块：category（分类）

| 类型 | 路径 |
|------|------|
| Controller | `module/category/controller/CategoryController.java` |
| Service | `CategoryService.java` / `impl/CategoryServiceImpl.java` |
| Entity | `entity/Category.java` |
| DTO | `dto/CategoryCreateDTO.java`, `CategoryUpdateDTO.java` |
| VO | `vo/CategoryVO.java` |
| Repository | `repository/CategoryRepository.java` |

**Mongo：** `@Document(collection = "suggestType")`（类名 `Category`，集合名 **suggestType**）。

**HTTP 示例前缀：** `/api/v1/categories`（见 `CategoryController`）。

- **入参名称：** JSON/表单均支持字段 **`name`**（与 `categoryName` 等价）；写接口支持 **`application/json`** 与 **`application/x-www-form-urlencoded`**。
- **前端请求头：** `Access-Token` / `X-Auth-Token`、`qg-tenant-id` 等由 `ClientRequestContextFilter` 解析至 `ClientRequestContext`（当前不强制鉴权）。
- **列表分页：** `GET .../categories/tree?pageNo=1&pageSize=20`（`pageNo` 从 1；`pageSize` 默认 20、最大 100）。**可选 `name`：** 对分类展示名子串模糊匹配（英文忽略大小写），再排序分页；参数名不区分大小写（如 `Name`）。**兼容：** `page` / `size`；`PageNo` 等与 `pageNo` 等价。`data` 为 `PageResult`。

## 运维 / 本地

- 端口：**8080**（`application.yml`）
- Swagger UI：`/swagger-ui.html`
- Actuator：`health`, `info`（见 `application.yml`）

## 新模块 checklist（复制 category）

1. `module/{新模块}/` 下补齐 controller、service、impl、entity、dto、vo、repository
2. Controller：`ApiResponse`、`@Valid`、SpringDoc、`@RateLimit`
3. Entity：`BaseEntity` + `@Document` + `@Field` / `@Indexed`
4. 测试：`{模块}ControllerTest`（WebMvcTest）+ `{模块}ServiceTest`（Mockito）
5. 若新增业务错误码：扩展 `BusinessCode`（及必要时 `SystemCode`）

## 上线发布流程

面向本仓库（Spring Boot 可执行 JAR + MongoDB），可按团队 CI/主机再填实括号内项。

### 1. 构建

| 步骤 | 说明 |
|------|------|
| 本地/流水线 | `mvn clean package`（发布前建议**不**跳过测试；仅排查时可 `-DskipTests`） |
| 产物 | `target/h5-xyqb-platform-1.0.0-SNAPSHOT.jar`（版本以 `pom.xml` 为准） |

### 2. 环境与配置

- 生产启动使用 **`spring.profiles.active=prod`**（与 `application-prod.yml` 对齐）。
- **MongoDB URI、密钥等** 通过环境变量或外部配置下发，**不入库、不进镜像层**。
- 确认生产库与 **`application-prod.yml`** 中 `spring.data.mongodb` 等项一致。

### 3. 运行示例

```bash
java -jar -Dspring.profiles.active=prod target/h5-xyqb-platform-*.jar
```

（JVM 参数、端口、用户等按运维规范追加。）

### 4. 发布前检查

- [ ] 目标 Mongo 可达，账号权限与库名正确。
- [ ] 索引策略：`application.yml` 中 `spring.data.mongodb.auto-index-creation` 在生产是否仍适用（若关闭，需提前建好索引）。
- [ ] 健康检查：`GET /actuator/health`（若经网关暴露，需与安全策略一致）。

### 5. 回滚

- 保留**上一版可执行 JAR**（或镜像 tag）；异常时切回旧制品并重启同一进程/实例。

### 6. 请团队补全（写入上表或链出文档）

- CI 平台与流水线位置（如 `.gitlab-ci.yml` / GitHub Actions）。
- 制品存放（Nexus/OCI 等）与部署形态（K8s Deployment、systemd、脚本路径）。
- 发布窗口、审批人、变更公告渠道（可选）。

## 待你补充（已知后写入本文）

- 各环境 MongoDB 连接信息约定（仅文档说明，不写密码）
- 是否还有 **H5 前端仓库** 路径或接口契约文档
