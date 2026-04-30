# Nacos Config Guide

本文档说明 `src/main/resources/application-nacos.yml` 当前导入哪些 Nacos 配置，以及每个配置文件应该写什么内容。

## 1. 当前 Import 规则

`application-nacos.yml` 只在显式启用 `nacos` profile 时加载。当前导入：

```yaml
spring:
  config:
    import:
      - optional:nacos:refinex-dbflow.yml?group=DBFLOW_GROUP&refreshEnabled=true
      - optional:nacos:refinex-dbflow-${spring.profiles.active}.yml?group=DBFLOW_GROUP&refreshEnabled=true
```

含义：

| Nacos dataId                                   | group          | 建议职责                                                                           |
|------------------------------------------------|----------------|--------------------------------------------------------------------------------|
| `refinex-dbflow.yml`                           | `DBFLOW_GROUP` | 所有环境共享的 DBFlow 基础配置、默认策略、占位符和安全默认值。                                            |
| `refinex-dbflow-${spring.profiles.active}.yml` | `DBFLOW_GROUP` | 当前 active profile 的覆盖配置，例如连接具体 metadata database、target project/env、可信 Origin。 |

如果启动参数是：

```bash
SPRING_PROFILES_ACTIVE=nacos
```

第二个 dataId 就是：

```text
refinex-dbflow-nacos.yml
```

本项目当前建议先按 `SPRING_PROFILES_ACTIVE=nacos` 使用。如果未来要区分 `dev`、`staging`、`prod`，建议单独调整
`application-nacos.yml` 的 dataId 命名策略后再启用多 profile，避免 `${spring.profiles.active}` 展开成逗号分隔值后产生难以发现的
dataId。

`optional:` 表示 Nacos 中缺失该 dataId 时应用不会因为 Config Data 缺失而直接启动失败；生产环境仍应通过启动检查和
`/admin/health` 确认配置是否按预期加载。

## 2. Nacos 连接参数

启动前设置：

```bash
export SPRING_PROFILES_ACTIVE=nacos
export DBFLOW_NACOS_SERVER_ADDR=127.0.0.1:8848
export DBFLOW_NACOS_NAMESPACE=
export DBFLOW_NACOS_USERNAME=nacos_user
read -rsp "Nacos password: " DBFLOW_NACOS_PASSWORD; echo
export DBFLOW_NACOS_PASSWORD
```

启动：

```bash
java -jar target/refinex-dbflow-0.1.0-SNAPSHOT.jar
```

Nacos 地址、用户名、密码、namespace 仍来自环境变量或密钥系统，不写入 Nacos 配置正文。

## 3. `refinex-dbflow.yml`：共享配置模板

在 Nacos 创建：

| 字段      | 值                    |
|---------|----------------------|
| Data ID | `refinex-dbflow.yml` |
| Group   | `DBFLOW_GROUP`       |
| Format  | YAML                 |

可复制内容：

```yaml
# 共享配置：适合放所有环境一致的默认值。
# 真实密码、Token pepper、管理员密码仍通过环境变量或密钥系统注入，不直接写在 Nacos 正文里。
spring:
  # JPA 只校验 Flyway 已创建的 DBFlow metadata schema，不允许 Hibernate 自动建表或改表。
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  # Flyway migration 只作用于 spring.datasource 指向的 DBFlow metadata database。
  # target project database 不会执行 classpath:db/migration 下的 DBFlow metadata migration。
  flyway:
    locations: classpath:db/migration

# Actuator 暴露面保持最小化；不要在内网部署中打开 env、beans 等敏感端点。
management:
  endpoints:
    web:
      exposure:
        include: health,metrics
  endpoint:
    health:
      show-details: never

dbflow:
  # target database 连接池默认值。具体业务库地址在 profile 覆盖配置的 dbflow.projects 中声明。
  datasource-defaults:
    driver-class-name: com.mysql.cj.jdbc.Driver
    # 默认 target DB 账号。不同环境可在单个 environment 下覆盖 username/password。
    username: ${DBFLOW_TARGET_USERNAME}
    password: ${DBFLOW_TARGET_PASSWORD}
    # 本地/灰度建议 false；生产若希望 target DB 不可达时启动失败，可设为 true。
    validate-on-startup: ${DBFLOW_TARGET_VALIDATE_ON_STARTUP:false}
    hikari:
      # pool name 会叠加 project/env，便于在日志和指标中识别目标库连接池。
      pool-name-prefix: dbflow-target
      maximum-pool-size: ${DBFLOW_TARGET_POOL_MAX_SIZE:10}
      minimum-idle: ${DBFLOW_TARGET_POOL_MIN_IDLE:1}
      connection-timeout: ${DBFLOW_TARGET_POOL_CONNECTION_TIMEOUT:30s}
      idle-timeout: ${DBFLOW_TARGET_POOL_IDLE_TIMEOUT:10m}
      max-lifetime: ${DBFLOW_TARGET_POOL_MAX_LIFETIME:30m}
  # 危险 SQL 默认策略。白名单通常放在 profile 覆盖配置中，按环境收紧。
  policies:
    dangerous-ddl:
      defaults:
        # DROP 默认拒绝，只有显式 whitelist 命中才可能放行。
        DROP_TABLE: DENY
        DROP_DATABASE: DENY
        # TRUNCATE 不直接执行，先返回服务端 confirmation challenge。
        TRUNCATE: REQUIRE_CONFIRMATION
      # 共享配置默认不放行任何 DROP；按环境在 refinex-dbflow-nacos.yml 中覆盖。
      whitelist: [ ]
  # 初始管理员只用于首次引导。生产建议使用 password-hash 或外部身份/密钥流程。
  admin:
    initial-user:
      enabled: ${DBFLOW_ADMIN_INITIAL_USER_ENABLED:false}
      username: ${DBFLOW_ADMIN_INITIAL_USERNAME:admin}
      display-name: ${DBFLOW_ADMIN_INITIAL_DISPLAY_NAME:DBFlow Administrator}
      # 明文密码仅适合一次性本地引导。生产建议留空，改用下方 password-hash。
      password: ${DBFLOW_ADMIN_INITIAL_PASSWORD:}
      # 生成命令见 docs/deployment/README.md 的“初始化管理员与 MCP Token Pepper”。
      # BCrypt hash 含 $，写入 shell/systemd/CI secret 时建议使用单引号包裹。
      password-hash: ${DBFLOW_ADMIN_INITIAL_PASSWORD_HASH:}
  security:
    # MCP Token pepper 是高敏感密钥，只能来自环境变量或密钥系统。
    mcp-token:
      pepper: ${DBFLOW_MCP_TOKEN_PEPPER}
    # /mcp endpoint 安全边界。Bearer Token 仍是每次请求必需认证，不接受 query string token。
    mcp-endpoint:
      origin:
        # 浏览器型客户端带 Origin 时会校验可信来源；CLI/Agent 通常不带 Origin。
        enabled: true
        trusted-origins:
          - ${DBFLOW_MCP_TRUSTED_ORIGIN:http://127.0.0.1:8080}
      request-size:
        # 防止客户端误传超大 SQL 或请求体。
        enabled: true
        max-bytes: ${DBFLOW_MCP_MAX_REQUEST_BYTES:1048576}
      rate-limit:
        # 基础源 IP 限流，避免交互式 MCP 被当作批处理通道。
        enabled: true
        max-requests: ${DBFLOW_MCP_RATE_LIMIT_MAX_REQUESTS:120}
        window: ${DBFLOW_MCP_RATE_LIMIT_WINDOW:1m}
```

适合放在共享配置里的内容：

- `spring.flyway.locations`
- `spring.jpa.hibernate.ddl-auto`
- Actuator 最小暴露策略
- target datasource 默认 Hikari 参数
- 危险 SQL 默认策略
- MCP endpoint request size / rate limit 默认值
- 只含环境变量占位的管理员初始用户和 Token pepper 配置

不适合放真实值：

- 数据库密码
- MCP Token pepper 明文
- Nacos 密码
- 管理员真实初始密码

## 4. `refinex-dbflow-nacos.yml`：当前 profile 覆盖模板

当 `SPRING_PROFILES_ACTIVE=nacos` 时，在 Nacos 创建：

| 字段      | 值                          |
|---------|----------------------------|
| Data ID | `refinex-dbflow-nacos.yml` |
| Group   | `DBFLOW_GROUP`             |
| Format  | YAML                       |

可复制内容：

```yaml
# profile 覆盖配置：适合放当前部署环境的 metadata database、监听地址、target project/env 和白名单。
# 当 SPRING_PROFILES_ACTIVE=nacos 时，当前 dataId 是 refinex-dbflow-nacos.yml。
spring:
  # DBFlow 自己的 metadata database，存用户、授权、Token hash、confirmation、audit 和 Flyway history。
  # V1__create_metadata_schema.sql 只会在这个 datasource 上执行。
  datasource:
    url: ${DBFLOW_METADATA_JDBC_URL}
    username: ${DBFLOW_METADATA_USERNAME:dbflow_meta}
    password: ${DBFLOW_METADATA_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

# 应用监听配置。内网部署建议应用只监听 127.0.0.1，由反向代理终止 TLS。
server:
  address: ${DBFLOW_SERVER_ADDRESS:127.0.0.1}
  port: ${DBFLOW_SERVER_PORT:8080}
  # 让 Spring 正确识别反向代理传入的 X-Forwarded-* 头。
  forward-headers-strategy: framework

dbflow:
  # target project/env 是 AI 通过 DBFlow 被授权访问的业务库清单。
  # 这些库不会执行 DBFlow Flyway migration，也不应出现 dbf_* 元数据表。
  projects:
    - key: demo
      name: Demo Project
      environments:
        # dev 示例：通常用于本地/测试查询和低风险策略验证。
        - key: dev
          name: Demo Dev
          jdbc-url: ${DBFLOW_DEMO_DEV_JDBC_URL}
          # 未单独设置时复用 datasource-defaults 中的 target DB 账号。
          username: ${DBFLOW_DEMO_DEV_USERNAME:${DBFLOW_TARGET_USERNAME}}
          password: ${DBFLOW_DEMO_DEV_PASSWORD:${DBFLOW_TARGET_PASSWORD}}
        # prod 示例：生产库应使用最小权限账号，并谨慎开启 validate-on-startup。
        - key: prod
          name: Demo Production
          jdbc-url: ${DBFLOW_DEMO_PROD_JDBC_URL}
          username: ${DBFLOW_DEMO_PROD_USERNAME:${DBFLOW_TARGET_USERNAME}}
          password: ${DBFLOW_DEMO_PROD_PASSWORD:${DBFLOW_TARGET_PASSWORD}}
  # 当前部署环境的危险 DDL 白名单。默认策略仍来自共享配置。
  policies:
    dangerous-ddl:
      whitelist:
        # 示例：只允许 demo/dev 的 scratch.tmp_* 表执行 DROP TABLE。
        # 不要把 prod DROP 白名单作为默认项；需要审批后再显式增加。
        - project-key: demo
          environment-key: dev
          schema-name: scratch
          table-name: tmp_*
          operation: DROP_TABLE
          # 即使通配命中，生产环境仍需要单独显式允许。
          allow-prod-dangerous-ddl: false
```

适合放在 profile 覆盖配置里的内容：

- metadata database 的 `spring.datasource.*` 占位配置。
- 当前部署环境的 `server.*`。
- 当前部署环境可用的 `dbflow.projects[*].environments[*]`。
- 当前环境的 DROP 白名单。
- 当前部署的 trusted Origin 占位值。

## 5. 启动前必须提供的环境变量

最小集合：

```bash
export DBFLOW_METADATA_JDBC_URL="jdbc:mysql://127.0.0.1:3306/dbflow_metadata?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=true&allowPublicKeyRetrieval=false"
export DBFLOW_METADATA_USERNAME=dbflow_meta
export DBFLOW_METADATA_PASSWORD="replace-with-secret"

export DBFLOW_TARGET_USERNAME=dbflow_operator
export DBFLOW_TARGET_PASSWORD="replace-with-secret"
export DBFLOW_DEMO_DEV_JDBC_URL="jdbc:mysql://127.0.0.1:3306/demo_dev?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false"
export DBFLOW_DEMO_PROD_JDBC_URL="jdbc:mysql://10.0.0.20:3306/demo_prod?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=true&allowPublicKeyRetrieval=false"

export DBFLOW_MCP_TOKEN_PEPPER="replace-with-secret"
export DBFLOW_MCP_TRUSTED_ORIGIN="https://dbflow.internal.example"
export DBFLOW_ADMIN_INITIAL_USER_ENABLED=true
export DBFLOW_ADMIN_INITIAL_USERNAME=admin
export DBFLOW_ADMIN_INITIAL_PASSWORD_HASH='$2a$10$replaceWithGeneratedBcryptHash'
```

示例里的 `replace-with-secret` 不是可用密码，只是占位符。

## 6. 验证配置是否生效

启动后检查：

```bash
curl -fsS http://127.0.0.1:8080/actuator/health
```

登录管理端后检查：

- `/admin/health`：metadata database、target datasource registry、Nacos、MCP endpoint readiness。
- `/admin/config`：确认 project/env 已显示，且不出现数据库密码。
- `/admin/policies/dangerous`：确认 DROP whitelist 和 TRUNCATE 策略符合预期。

如果 target datasource registry 为空，优先检查 `refinex-dbflow-nacos.yml` 是否包含 `dbflow.projects`，以及相关
`DBFLOW_DEMO_*_JDBC_URL` 环境变量是否存在。
