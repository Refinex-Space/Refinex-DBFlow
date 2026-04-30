# Refinex-DBFlow Deployment Guide

本文档面向本地开发、内网测试和 MVP 级 jar 部署。DBFlow 是内部 MySQL MCP 网关，生产化部署必须把元数据库、目标
MySQL、Nacos、TLS、密钥和网络访问控制放在仓库外部管理。

## 1. 前置条件

| 项目      | 要求                              | 验证命令                                                           |
|---------|---------------------------------|----------------------------------------------------------------|
| JDK     | 21                              | `java -version`                                                |
| 构建工具    | 使用仓库内 Maven Wrapper             | `./mvnw -version`                                              |
| 本地空环境启动 | 不需要 MySQL，不需要 Nacos             | 默认 profile 使用 H2 元数据库                                          |
| 内网部署    | MySQL 8 元数据库、目标 MySQL、反向代理或内网入口 | 由运维环境提供                                                        |
| Nacos   | 仅在启用 `nacos` profile 时需要        | `curl -fsS http://127.0.0.1:8848/nacos/v1/ns/operator/metrics` |

所有命令默认在仓库根目录执行：

```bash
cd /Users/refinex/develop/code/Refinex-DBFlow
```

## 2. 从空环境启动本地开发实例

前置条件：本机已安装 JDK 21，8080 端口未被其它进程占用；不要求 MySQL 或 Nacos。

```bash
java -version
./mvnw -q -DskipTests package
SERVER_PORT=18080 java -jar target/refinex-dbflow-0.1.0-SNAPSHOT.jar > target/dbflow-local.log 2>&1 &
APP_PID=$!
sleep 20
curl -fsS http://localhost:18080/actuator/health
kill "$APP_PID"
wait "$APP_PID" 2>/dev/null || true
```

预期：`curl` 返回 `{"status":"UP"}`。本地默认使用 H2 内存元数据库，Nacos 关闭，目标项目环境可为空，适合验证
应用能启动、Actuator 最小暴露可访问、Flyway 元数据迁移可执行。

开发期间也可以直接运行：

```bash
./mvnw spring-boot:run
```

## 3. 构建可部署 jar

前置条件：JDK 21 可用，Maven 依赖可从网络或本地缓存解析。

```bash
./mvnw test
./mvnw -DskipTests package
ls -lh target/refinex-dbflow-0.1.0-SNAPSHOT.jar
```

`./mvnw test` 是发布前必跑命令；Testcontainers 集成测试在本机无 Docker runtime 时会自动跳过。最终部署物是 Spring
Boot 可执行 jar：

```bash
java -jar target/refinex-dbflow-0.1.0-SNAPSHOT.jar
```

## 4. 外部配置文件

仓库内的 `src/main/resources/application.yml` 是本地默认配置。部署时不要修改 jar 内配置，使用外部目录覆盖：

```bash
mkdir -p config
cp docs/deployment/application-dbflow-example.yml config/application-dbflow.yml
```

启动时加载外部 profile：

```bash
SPRING_PROFILES_ACTIVE=dbflow \
SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:./config/ \
java -jar target/refinex-dbflow-0.1.0-SNAPSHOT.jar
```

`application-dbflow-example.yml` 只包含环境变量占位。填值方式应是 shell 环境变量、systemd `EnvironmentFile`、Nacos
外部配置、Vault/KMS 或其它密钥系统，不能把真实密码写回仓库。

## 5. 配置元数据库

本地默认用 H2；内网部署建议使用独立 MySQL 8 schema 存储 DBFlow 元数据、审计、Token hash 和确认挑战。

前置条件：已安装 MySQL 客户端，能以管理员身份连接目标 MySQL。

```bash
export MYSQL_HOST=127.0.0.1
export MYSQL_PORT=3306
export DBFLOW_METADATA_PASSWORD="$(openssl rand -base64 24)"
mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u root -p <<SQL
CREATE DATABASE IF NOT EXISTS dbflow_metadata CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'dbflow_meta'@'%' IDENTIFIED BY '${DBFLOW_METADATA_PASSWORD}';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
  ON dbflow_metadata.* TO 'dbflow_meta'@'%';
FLUSH PRIVILEGES;
SQL
```

导出 DBFlow 元数据库连接参数：

```bash
export DBFLOW_METADATA_JDBC_URL="jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/dbflow_metadata?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=true&allowPublicKeyRetrieval=false"
export DBFLOW_METADATA_USERNAME=dbflow_meta
export DBFLOW_METADATA_PASSWORD
```

注意：JDBC URL 里不要写 `password=`。密码只通过 `DBFLOW_METADATA_PASSWORD` 或外部密钥系统注入。

## 6. 配置 MySQL 项目环境

目标 MySQL 是 DBFlow 要代理操作的业务库。每个环境必须配置在 `dbflow.projects[*].environments[*]` 下，执行前还要在
管理端给用户授予 project/env 权限。

最小环境变量示例：

```bash
export DBFLOW_TARGET_USERNAME=dbflow_operator
export DBFLOW_TARGET_PASSWORD="$(openssl rand -base64 24)"
export DBFLOW_DEMO_DEV_JDBC_URL="jdbc:mysql://127.0.0.1:3306/demo_dev?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false"
export DBFLOW_DEMO_PROD_JDBC_URL="jdbc:mysql://10.0.0.20:3306/demo_prod?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=true&allowPublicKeyRetrieval=false"
export DBFLOW_MCP_TRUSTED_ORIGIN=http://127.0.0.1:8080
```

目标库账号建议按最小权限创建：只给允许被 AI 工具操作的 schema 权限，不复用 DBA/root 账号。
`dbflow.datasource-defaults.validate-on-startup`
本地建议保持 `false`；生产若希望目标库不可达时启动失败，可设为 `true`。

## 7. 初始化管理员与 MCP Token Pepper

前置条件：准备一个只用于 DBFlow 的管理员初始密码，或准备 BCrypt hash。下面命令只把随机值放入当前 shell，不写文件。

```bash
export DBFLOW_ADMIN_INITIAL_USER_ENABLED=true
export DBFLOW_ADMIN_INITIAL_USERNAME=admin
export DBFLOW_ADMIN_INITIAL_PASSWORD="$(openssl rand -base64 24)"
export DBFLOW_MCP_TOKEN_PEPPER="$(openssl rand -base64 32)"
```

启动并首次登录 `/login` 后，应在管理端创建个人管理员/用户，完成授权与 MCP Token 颁发，再关闭初始化账号或改用
`DBFLOW_ADMIN_INITIAL_PASSWORD_HASH`。MCP Token 明文只在颁发成功页展示一次；丢失后重新颁发。

## 8. Nacos 配置

默认 profile 不连接 Nacos。启用 Nacos 需要显式打开 `nacos` profile，并提供 Nacos 地址、命名空间和认证信息。

前置条件：Nacos 2.x 服务已启动，账号具备读取 `DBFLOW_GROUP` 配置和注册服务权限。

```bash
export SPRING_PROFILES_ACTIVE=nacos
export DBFLOW_NACOS_SERVER_ADDR=127.0.0.1:8848
export DBFLOW_NACOS_NAMESPACE=
export DBFLOW_NACOS_USERNAME=nacos_user
read -rsp "Nacos password: " DBFLOW_NACOS_PASSWORD; echo
export DBFLOW_NACOS_PASSWORD
java -jar target/refinex-dbflow-0.1.0-SNAPSHOT.jar
```

Nacos 配置文件名：

- `refinex-dbflow.yml`
- `refinex-dbflow-${spring.profiles.active}.yml`

配置 group 固定为 `DBFLOW_GROUP`。Nacos 中也不要保存真实密码明文，优先使用环境变量占位、Nacos 自身加密能力或外部密钥系统。

## 9. 启动参数与 systemd 示例

单机 jar 启动：

```bash
SERVER_PORT=8080 \
SPRING_PROFILES_ACTIVE=dbflow \
SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:./config/ \
java -jar target/refinex-dbflow-0.1.0-SNAPSHOT.jar
```

Linux systemd 示例前置条件：jar 已放到 `/opt/refinex-dbflow/refinex-dbflow.jar`，外部配置在 `/etc/refinex-dbflow/`，
密钥环境文件在 `/etc/refinex-dbflow/dbflow.env`，运行用户 `dbflow` 已存在。

```ini
[Unit]
Description=Refinex DBFlow
After=network-online.target
Wants=network-online.target

[Service]
User=dbflow
Group=dbflow
WorkingDirectory=/opt/refinex-dbflow
EnvironmentFile=/etc/refinex-dbflow/dbflow.env
Environment=SPRING_PROFILES_ACTIVE=dbflow
Environment=SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:/etc/refinex-dbflow/
ExecStart=/usr/bin/java -jar /opt/refinex-dbflow/refinex-dbflow.jar
Restart=on-failure
RestartSec=5
NoNewPrivileges=true

[Install]
WantedBy=multi-user.target
```

`dbflow.env` 必须由运维密钥流程生成，文件权限建议为 `0600`，不要提交到仓库。

## 10. 反向代理与 TLS

建议 DBFlow 进程只监听 `127.0.0.1:8080`，由内网反向代理终止 TLS，并只开放 HTTPS 入口。示例 YAML 已设置
`server.forward-headers-strategy=framework`，用于识别代理转发头。

Nginx 示例前置条件：Nginx 已安装，证书文件已存在，DNS 或 hosts 已指向内网主机。

```nginx
server {
    listen 443 ssl http2;
    server_name dbflow.internal.example;

    ssl_certificate /etc/nginx/tls/dbflow.crt;
    ssl_certificate_key /etc/nginx/tls/dbflow.key;
    ssl_protocols TLSv1.2 TLSv1.3;

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

如果浏览器型 MCP 客户端会带 `Origin`，把代理域名加入可信来源：

```bash
export DBFLOW_MCP_TRUSTED_ORIGIN=https://dbflow.internal.example
```

CLI/Agent MCP 客户端通常不带 `Origin`，仍需要每次请求带 `Authorization: Bearer <DBFlow Token>`。

## 11. 内网访问限制

DBFlow 不应暴露到公网。推荐组合：

- 进程监听 `127.0.0.1`，只让反向代理访问应用端口。
- 反向代理只绑定内网地址或 VPN 网段。
- 防火墙只放行内网 CIDR 到 443，不放行 8080。
- `/actuator/health` 和 `/actuator/metrics` 维持最小暴露，不开启 `/actuator/env`。
- `/mcp` 不接受 query string token，必须使用 Bearer Token。

Ubuntu `ufw` 示例前置条件：系统启用 `ufw`，网段 `10.0.0.0/8` 是你的可信内网。

```bash
sudo ufw default deny incoming
sudo ufw allow from 10.0.0.0/8 to any port 443 proto tcp
sudo ufw deny 8080/tcp
sudo ufw status verbose
```

验证入口：

```bash
curl -fsS https://dbflow.internal.example/actuator/health
curl -i https://dbflow.internal.example/mcp
```

第二个命令没有 Bearer Token 时应返回 401，而不是连接到未受保护的业务接口。

## 12. 健康检查与指标

默认暴露面：

- `/actuator/health`
- `/actuator/metrics`
- `/actuator/metrics/{name}`

验证命令：

```bash
curl -fsS http://localhost:8080/actuator/health
curl -fsS http://localhost:8080/actuator/metrics
```

健康详情默认隐藏，敏感配置不会通过 Actuator 返回。管理端 `/admin/health` 复用同一套健康服务，但需要管理员登录。

## 13. MVP 暂不提供 Dockerfile

本阶段不提交 Dockerfile。原因是当前 MVP 的生产可用性主要取决于仓库外部的内网部署边界：MySQL 元数据库、目标 MySQL
账号权限、Nacos、TLS 证书、Token pepper、管理员初始凭据、反向代理和防火墙。此时提供一个通用容器镜像容易掩盖这些
前置条件，也不能解决密钥注入和内网访问控制。

后续若进入容器化阶段，至少需要同时补齐：

- 非 root 运行用户与只读文件系统策略。
- 外部配置和密钥挂载约定。
- 健康检查 endpoint 与启动探针。
- 镜像 SBOM/漏洞扫描流程。
- 反向代理或 Ingress TLS 与内网访问策略。

## 14. 部署前检查清单

```bash
./mvnw test
./mvnw -DskipTests package
python3 scripts/check_harness.py
```

人工检查：

- 外部配置文件不含真实密码、Token 明文、Token pepper 或 Nacos 密码。
- JDBC URL 不含 `password=`。
- 初始化管理员只用于首次引导，完成后关闭或改用受管 hash。
- 目标 MySQL 账号不是 root/DBA 账号。
- 生产环境 `DROP_TABLE`、`DROP_DATABASE` 默认拒绝，`TRUNCATE` 仍需确认挑战。
- 反向代理启用 TLS，应用端口不直接暴露到公网。
