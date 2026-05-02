# Refinex-DBFlow Deployment

这份文档只保留一条主路径：复制 Nacos dev YAML，启动应用，使用 `admin/admin` 登录，随后在管理端修改密码并按需添加
project/env。首次启动不需要提前准备示例业务库环境变量。

## 1. 前置条件

| 项目    | 要求                                 | 验证命令                                                           |
|-------|------------------------------------|----------------------------------------------------------------|
| JDK   | 21                                 | `java -version`                                                |
| Maven | 使用仓库内 Maven Wrapper                | `./mvnw -version`                                              |
| Nacos | dev 路径需要 Nacos 2.x                 | `curl -fsS http://127.0.0.1:8848/nacos/v1/ns/operator/metrics` |
| MySQL | 首次跑通不需要；配置 target project/env 时再准备 | `mysql --version`                                              |

所有命令默认在仓库根目录执行：

```bash
cd /Users/refinex/develop/code/Refinex-DBFlow
```

## 2. 推荐启动方式：Nacos dev

`src/main/resources/application.yml` 默认会导入：

```text
optional:nacos:application-dbflow.yml?group=DBFLOW_GROUP&refreshEnabled=true
```

dev 只需要先建一个 Data ID。不要再拆共享配置、profile 配置或本地外部 YAML：

| 字段      | 值                                                                       |
|---------|-------------------------------------------------------------------------|
| Data ID | `application-dbflow.yml`                                                |
| Group   | `DBFLOW_GROUP`                                                          |
| Format  | YAML                                                                    |
| 内容      | 复制 [nacos/dev/application-dbflow.yml](nacos/dev/application-dbflow.yml) |

启动：

```bash
set -a
source .env.example
set +a
./mvnw spring-boot:run
```

访问：

| 入口     | 地址                                      |
|--------|-----------------------------------------|
| 管理端    | `http://127.0.0.1:8080/login`           |
| 初始账号   | `admin`                                 |
| 初始密码   | `admin`                                 |
| Health | `http://127.0.0.1:8080/actuator/health` |
| MCP    | `http://127.0.0.1:8080/mcp`             |

首次登录后请立即修改管理员密码，再创建个人用户、授权 project/env、颁发 MCP Token。

## 3. 配置 project/env

目标业务库不通过 `.env.example` 配置，直接在 `dbflow.projects` 中维护。把
[nacos/dev/application-dbflow.yml](nacos/dev/application-dbflow.yml) 里的示例结构取消注释并改成自己的库即可：

```yaml
dbflow:
  projects:
    - key: your-project
      name: Your Project
      environments:
        - key: dev
          name: Dev
          jdbc-url: jdbc:mysql://127.0.0.1:3306/your_schema?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
          username: your_dbflow_user
          password: your_dbflow_password
```

建议：

- target MySQL 账号使用最小权限，不复用 DBA/root。
- JDBC URL 不写 `password=`。
- `prod` 环境不要默认配置 DROP 白名单。
- `TRUNCATE` 默认需要服务端 confirmation challenge。

## 4. 元数据库

dev Nacos YAML 默认配置 H2 内存库，适合把管理端和 MCP endpoint 跑起来；应用重启后元数据会丢失。

如果要使用 MySQL 作为 DBFlow 元数据库，在同一个 Nacos YAML 中替换 `spring.datasource` 即可。Flyway migration
`src/main/resources/db/migration/V1__create_metadata_schema.sql` 只会在 `spring.datasource` 指向的 DBFlow metadata
database
执行，不会在 `dbflow.projects` 里的 target database 执行。

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/dbflow_metadata?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: dbflow_meta
    password: dbflow_meta_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

最小建库 SQL：

```sql
CREATE DATABASE IF NOT EXISTS dbflow_metadata CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE
USER IF NOT EXISTS 'dbflow_meta'@'%' IDENTIFIED BY 'replace-with-secret';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
  ON dbflow_metadata.* TO 'dbflow_meta'@'%';
FLUSH PRIVILEGES;
```

## 5. 构建 jar

```bash
./mvnw test
./mvnw -DskipTests package
ls -lh target/refinex-dbflow-0.1.0-SNAPSHOT.jar
```

Testcontainers 集成测试在本机无 Docker runtime 时会自动跳过。

jar 启动：

```bash
set -a
source .env.example
set +a
java -jar target/refinex-dbflow-0.1.0-SNAPSHOT.jar
```

## 6. 内网部署注意

- DBFlow 不应暴露公网。
- 建议应用只监听内网或 `127.0.0.1`，由内网反向代理终止 TLS。
- `/mcp` 不接受 query string token，只接受 Bearer Token。
- 不要提交真实数据库密码、MCP Token、Token pepper 或 Nacos 密码。
- 生产应把 `admin/admin` 改成受管管理员账号，并轮换 `dbflow.security.mcp-token.pepper`。

### 单实例容量治理

DBFlow 默认按单实例内网部署设计容量保护。`dbflow.security.mcp-endpoint.*` 先做 Origin、request size 和来源 IP
粗保护；通过认证和授权后，`dbflow.capacity.*` 会继续按 Token、用户、工具类别、target project/env 做限流和并发
bulkhead。流量突增时：

- `EXECUTE` 和 `EXPLAIN` 默认快速返回容量拒绝，不排长队。
- `HEAVY_READ` 默认降级，把 schema inspect 之类的 `maxItems` 下调到 50。
- 响应会带 `error.reasonCode`、`retryAfterMillis` 和 `notices`，客户端应按提示稍后重试。
- 管理端健康页会显示“容量治理”，Actuator metrics 暴露 `dbflow.capacity.*` 指标。

建议先保持模板默认值，压测 100 个逻辑用户混合调用后再调整 `global-max-concurrent`、`EXECUTE.max-concurrent`、
`per-token-max-concurrent` 和目标 Hikari `maximum-pool-size`。容量上限不要大于目标库和 metadata database 能承受的连接预算。

### Tomcat 与虚拟线程

模板显式配置了 `server.tomcat.threads.max`、`server.tomcat.max-connections` 和 `server.tomcat.accept-count`。反向代理的
`client_max_body_size` 应与 `dbflow.security.mcp-endpoint.request-size.max-bytes` 对齐，避免代理和应用层限制不一致。

JDK 21 虚拟线程保持 opt-in：

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

虚拟线程可以降低请求线程占用，但不会放大 Hikari pool、SQL timeout、bulkhead 或限流阈值。启用前同时压测
`spring.threads.virtual.enabled=false` 和 `true`，并观察 pinned thread、目标池 waiting、`dbflow.capacity.rejections`
和 SQL duration。

Nginx 反向代理示例：

```nginx
server {
    listen 443 ssl http2;
    server_name dbflow.internal.example;

    ssl_certificate /etc/nginx/tls/dbflow.crt;
    ssl_certificate_key /etc/nginx/tls/dbflow.key;
    client_max_body_size 1m;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Request-Id $request_id;
    }
}
```

如果浏览器型 MCP 客户端会带 `Origin`，把代理域名加入 `dbflow.security.mcp-endpoint.origin.trusted-origins`。

## 7. 相关文档

- [../user-guide/admin-guide.md](../user-guide/admin-guide.md) - 管理员登录、用户、授权、Token、审计和健康状态。
- [../user-guide/mcp-clients.md](../user-guide/mcp-clients.md) - Codex、Claude、OpenCode、Copilot MCP 客户端配置。
- [../runbooks/troubleshooting.md](../runbooks/troubleshooting.md) - 启动、Nacos、数据库、Token 和 MCP 连接排障。
