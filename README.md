# Refinex DBFlow

Refinex-DBFlow 是面向企业内网的 MySQL MCP 数据库操作网关。它让 Codex、Claude、OpenCode、Copilot 等 AI
客户端通过 MCP Streamable HTTP 访问被授权的 MySQL 项目环境，同时由服务端统一执行认证、授权、SQL 风险分类、危险操作策略、
确认挑战、限流、审计和运维观测。

项目当前形态是 Spring Boot WebMVC 单体应用，不引入独立前端 SPA。管理端使用 Spring MVC + Thymeleaf，MCP endpoint 使用
Spring AI MCP WebMVC Server，目标数据库连接由 HikariCP 按 project/env 维护。

## 适用场景

- 企业内网中希望让 AI 工具安全查询、解释和操作 MySQL。
- 需要把 AI 访问数据库的行为变成可授权、可拒绝、可确认、可审计的服务端流程。
- 需要给管理员提供用户、Token、项目环境授权、危险策略、审计和健康状态页面。
- 需要支持 Codex、Claude、OpenCode、Copilot 等 MCP 客户端的统一接入手册。

不适合的场景：

- 公网直接暴露数据库操作入口。
- 把 DBA/root 账号交给 AI 客户端。
- 依赖客户端提示词约束危险 SQL，而不做服务端策略。

## 核心能力

- **MCP Streamable HTTP**：默认 endpoint 为 `http://localhost:8080/mcp`，请求必须携带
  `Authorization: Bearer <DBFlow Token>`。
- **管理端**：`/login` 登录，`/admin` 进入后台；支持用户、Token、project/env 授权、配置查看、危险策略、审计、健康状态。
- **受控 SQL 执行**：`SELECT`、`SHOW`、`DESCRIBE`、`EXPLAIN` 返回限流结果；DML/DDL 返回 affected rows、warning、duration
  和 statement summary。
- **执行计划与 schema inspect**：支持 `dbflow_explain_sql`、`dbflow_inspect_schema` 和 schema resource，面向 MCP 客户端返回稳定字段。
- **服务端安全策略**：先授权、再分类和策略、再执行、最后审计；`DROP TABLE`、`DROP DATABASE` 默认拒绝，`TRUNCATE` 必须走确认挑战。
- **审计与观测**：审计记录包含用户、Token 元数据、客户端、project/env、tool、operation、risk、decision、SQL hash 和受限结果摘要；
  Actuator 最小暴露 health/metrics，日志带 `requestId` 和 `traceId`。

## 技术栈

| 领域            | 选型                                                                                 |
|---------------|------------------------------------------------------------------------------------|
| Runtime       | JDK 21                                                                             |
| Framework     | Spring Boot 3.5.13, Spring MVC, Spring Security, Thymeleaf                         |
| MCP           | Spring AI 1.1.4 MCP WebMVC Server, Streamable HTTP                                 |
| Database      | MySQL target environments, H2 dev metadata fallback from Nacos YAML                |
| Persistence   | Flyway, Spring Data JPA                                                            |
| SQL           | JSQLParser 5.3, Spring JDBC, HikariCP                                              |
| Config        | Nacos Config/Discovery by default, Spring external placeholders for Nacos access   |
| Observability | Spring Boot Actuator, Micrometer, structured logs                                  |
| Test          | JUnit 5, Spring Boot Test, Testcontainers for MySQL 8/5.7 when Docker is available |

## 快速开始

前置条件：JDK 21 可用，并准备可访问的 Nacos dev 配置。首次 smoke 不需要准备 target MySQL。

```bash
cd /Users/refinex/develop/code/Refinex-DBFlow
java -version
./mvnw test
set -a
source .env.example
set +a
./mvnw spring-boot:run
```

启动后常用入口：

| 入口           | 地址                                       |
|--------------|------------------------------------------|
| 管理端登录        | `http://localhost:8080/login`            |
| 管理端首页        | `http://localhost:8080/admin`            |
| MCP endpoint | `http://localhost:8080/mcp`              |
| Health       | `http://localhost:8080/actuator/health`  |
| Metrics      | `http://localhost:8080/actuator/metrics` |

本地 Nacos dev 启动、MySQL 元数据库、反向代理/TLS 和内网访问限制请从
[部署手册](docs/deployment/README.md) 开始，不要直接修改 jar 内的 `src/main/resources/application.yml`。

## 常用命令

| 任务              | 命令                                                            |
|-----------------|---------------------------------------------------------------|
| 安装依赖到本地缓存       | `./mvnw dependency:go-offline`                                |
| 运行测试            | `./mvnw test`                                                 |
| 构建 jar          | `./mvnw -DskipTests package`                                  |
| 本地启动（Nacos dev） | `set -a; source .env.example; set +a; ./mvnw spring-boot:run` |
| 校验 Harness 控制面  | `python3 scripts/check_harness.py`                            |

`./mvnw test` 在没有 Docker runtime 的机器上会自动跳过 Testcontainers 集成测试；这不是失败。

## 配置与密钥边界

仓库提供 [.env.example](.env.example) 指定 Nacos 连接，业务配置直接维护在
[docs/deployment/nacos/dev/application-dbflow.yml](docs/deployment/nacos/dev/application-dbflow.yml) 对应的 Nacos
Data ID 中。真实生产数据库密码、Token、Token pepper、Nacos 密码必须放在受控配置或密钥系统中，不能提交到仓库。

关键边界：

- `V1__create_metadata_schema.sql` 只在 DBFlow metadata database 执行，不在任何 target project database 执行。
- JDBC URL 不应包含 `password=` 参数。
- MCP Token 明文只在管理端颁发成功时展示一次；服务端只保存 hash 和元数据。
- dev 配置默认初始化 `admin/admin`；首次登录后应立即修改密码。
- target datasource、危险策略、管理员引导、Token pepper 和 MCP 安全策略应通过 Nacos YAML 管理。

## MCP 工具面

当前 MCP 工具和资源面向客户端保持稳定字段：

| 名称                            | 用途                                             |
|-------------------------------|------------------------------------------------|
| `dbflow_smoke`                | 检查 MCP Server 是否可达。                            |
| `dbflow_list_targets`         | 列出当前 Token 可访问的 project/env。                   |
| `dbflow_inspect_schema`       | 查询 schema/table/column/index/view/routine 元数据。 |
| `dbflow_get_effective_policy` | 查看当前 project/env 的有效危险 SQL 策略。                 |
| `dbflow_explain_sql`          | 对 SELECT 和可 explain 的 DML 生成执行计划，不实际执行目标 DML。  |
| `dbflow_execute_sql`          | 执行受控 SQL，查询结果默认截断，DML/DDL 返回执行摘要。              |
| `dbflow_confirm_sql`          | 确认并消费服务端生成的高风险 SQL challenge。                  |
| `dbflow_targets` resource     | 暴露可访问目标库清单。                                    |
| `dbflow_schema` resource      | 暴露 schema inspect 资源。                          |
| `dbflow_policy` resource      | 暴露策略视图。                                        |

客户端配置和首次 smoke prompt 见 [MCP 客户端手册](docs/user-guide/mcp-clients.md)。

## 文档导航

按角色阅读：

| 角色     | 起点                                                                                                                |
|--------|-------------------------------------------------------------------------------------------------------------------|
| 部署/运维  | [部署手册](docs/deployment/README.md), [Nacos dev YAML](docs/deployment/nacos/dev/application-dbflow.yml)             |
| 管理员    | [管理员手册](docs/user-guide/admin-guide.md)                                                                           |
| 员工/操作者 | [员工使用手册](docs/user-guide/operator-guide.md), [MCP 客户端手册](docs/user-guide/mcp-clients.md)                          |
| 安全/审计  | [安全与审计说明](docs/user-guide/security-and-audit.md), [审计/观测说明](docs/OBSERVABILITY.md)                                |
| 排障人员   | [故障排查 Runbook](docs/runbooks/troubleshooting.md)                                                                  |
| 开发者    | [架构说明](docs/ARCHITECTURE.md), [Java 开发规范](docs/references/java-development-standards.md), [执行计划索引](docs/PLANS.md) |
| 产品/设计  | [后台管理原型说明](docs/prototypes/admin/README.md), [后台 HTML 原型](docs/prototypes/admin/index.html)                       |

关键文档：

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) - 当前模块地图、架构边界和已实现能力。
- [docs/OBSERVABILITY.md](docs/OBSERVABILITY.md) - 测试、运行、观测、日志、指标和安全边界。
- [docs/deployment/README.md](docs/deployment/README.md) - Nacos dev 启动、元数据库、project/env、TLS、内网访问限制。
- [docs/deployment/nacos/dev/application-dbflow.yml](docs/deployment/nacos/dev/application-dbflow.yml) - 可直接复制到
  Nacos
  Data ID `application-dbflow.yml` 的 dev 配置。
- [docs/user-guide/mcp-clients.md](docs/user-guide/mcp-clients.md) - Codex、Claude、OpenCode、Copilot MCP 配置和 smoke
  prompt。
- [docs/user-guide/admin-guide.md](docs/user-guide/admin-guide.md) - 登录、用户、授权、Token、审计、策略、连接排查。
- [docs/user-guide/operator-guide.md](docs/user-guide/operator-guide.md) - 员工申请 Token、配置 MCP、执行查询、处理确认和拒绝原因。
- [docs/user-guide/security-and-audit.md](docs/user-guide/security-and-audit.md) - AI 操作数据库为什么不是黑盒，以及审计字段价值。
- [docs/runbooks/troubleshooting.md](docs/runbooks/troubleshooting.md) - 启动失败、Nacos、数据库、Token、MCP、策略和 SQL
  执行故障排查。
- [0-1-PLANS.md](0-1-PLANS.md) - 根级 0-1 路线图；[docs/PLANS.md](docs/PLANS.md) 是 Harness 执行计划索引。

## 目录速览

```text
.
+-- src/main/java/com/refinex/dbflow/   # Spring Boot 应用、MCP、SQL、审计、安全、管理端、观测
+-- src/main/resources/db/migration/    # DBFlow metadata database Flyway migration
+-- src/main/resources/templates/admin/ # Thymeleaf 管理端页面
+-- src/main/resources/static/          # 管理端静态资源
+-- docs/deployment/                    # 部署说明和可复制 dev 配置
+-- docs/user-guide/                    # 管理员、员工、MCP 客户端、安全审计手册
+-- docs/runbooks/                      # 可执行排障手册
+-- docs/exec-plans/                    # Harness specs、active plans、completed plans
+-- docs/prototypes/admin/              # 后台管理 HTML 高保真原型
```

## 开发约定

- 后端代码和配置注释遵循 [Java 开发规范](docs/references/java-development-standards.md)。
- 设计决策写入 `docs/exec-plans/specs/`；实现计划写入 `docs/exec-plans/active/`，完成后归档到
  `docs/exec-plans/completed/`。
- `docs/PLANS.md` 是 Harness 计划索引；完整产品路线图在 [0-1-PLANS.md](0-1-PLANS.md)。
- 修改完成前至少运行相关测试；声明完成前运行 `python3 scripts/check_harness.py`，需要全量回归时运行 `./mvnw test`。

## License

See [LICENSE](LICENSE).
