# Metadata Database And Flyway

本文档回答两个部署问题：`V1__create_metadata_schema.sql` 应该在哪个 database 执行，以及是否应该把 database
初始化写进 Flyway migration。

## 1. 结论

`src/main/resources/db/migration/V1__create_metadata_schema.sql` 只应该在 DBFlow 自己的 metadata database 上执行。
它不应该在任何被 AI 操作的 target project database 上执行。

当前 Spring Boot/Flyway 边界：

| 项目              | 当前配置                                                             | 含义                                         |
|-----------------|------------------------------------------------------------------|--------------------------------------------|
| migration 文件    | `src/main/resources/db/migration/V1__create_metadata_schema.sql` | DBFlow 元数据表结构。                             |
| Flyway 位置       | `spring.flyway.locations=classpath:db/migration`                 | 从 jar classpath 读取 migration。              |
| 默认本地 database   | `jdbc:h2:mem:dbflow_metadata`                                    | 本地开发和测试使用 H2 MySQL mode。                   |
| 内网部署 database   | `spring.datasource.url=${DBFLOW_METADATA_JDBC_URL}`              | 部署时指向独立 MySQL metadata schema。             |
| target database | `dbflow.projects[*].environments[*].jdbc-url`                    | AI 查询/执行的业务库，不跑 DBFlow metadata migration。 |

Spring Boot 默认让 Flyway 使用应用 primary `DataSource`。在 DBFlow 中，这个 primary `DataSource` 就是
`spring.datasource`，也就是 metadata database。target project databases 由 `dbflow.projects[*].environments[*]`
创建 Hikari pool，执行 SQL 时才使用，不参与 Flyway auto migration。

## 2. 不建议在 V1 中写 CREATE DATABASE / USE

不要把下面内容放进 `V1__create_metadata_schema.sql`：

```sql
CREATE
DATABASE dbflow_metadata;
USE
dbflow_metadata;
CREATE
USER 'dbflow_meta'@'%';
GRANT
...;
```

原因：

- Flyway migration 应该只管理“当前连接 database/schema”里的对象，便于本地 H2、测试和 MySQL 部署共用同一套迁移。
- `CREATE DATABASE`、`CREATE USER`、`GRANT` 需要更高权限，不应该让应用运行账号长期持有。
- `USE dbflow_metadata` 会让 migration 与连接上下文耦合，容易在测试或不同 MySQL 部署里跑偏。
- Flyway history table 应该留在同一个 metadata database 中，记录 DBFlow schema 版本。

更稳的做法是：先由 DBA/运维执行一次 bootstrap SQL 创建 database 和低权限账号，然后 DBFlow 启动时让 Flyway 在这个
database 中创建/升级 `dbf_*` 表。

## 3. Copy-ready MySQL 8 Bootstrap

前置条件：

- 已安装 MySQL client。
- 当前账号有 `CREATE DATABASE`、`CREATE USER`、`GRANT` 权限。
- 下面变量只存在于当前 shell，不写入仓库。

```bash
export MYSQL_HOST=127.0.0.1
export MYSQL_PORT=3306
export DBFLOW_METADATA_DATABASE=dbflow_metadata
export DBFLOW_METADATA_USERNAME=dbflow_meta
export DBFLOW_METADATA_PASSWORD="$(openssl rand -base64 24)"
```

执行 bootstrap：

```bash
mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u root -p <<SQL
CREATE DATABASE IF NOT EXISTS ${DBFLOW_METADATA_DATABASE}
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS '${DBFLOW_METADATA_USERNAME}'@'%'
  IDENTIFIED BY '${DBFLOW_METADATA_PASSWORD}';

GRANT SELECT, INSERT, UPDATE, DELETE,
      CREATE, ALTER, INDEX, REFERENCES
  ON ${DBFLOW_METADATA_DATABASE}.*
  TO '${DBFLOW_METADATA_USERNAME}'@'%';

FLUSH PRIVILEGES;
SQL
```

导出 DBFlow metadata 连接：

```bash
export DBFLOW_METADATA_JDBC_URL="jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${DBFLOW_METADATA_DATABASE}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=true&allowPublicKeyRetrieval=false"
export DBFLOW_METADATA_USERNAME
export DBFLOW_METADATA_PASSWORD
```

注意：

- JDBC URL 不写 `password=`。
- `DBFLOW_METADATA_PASSWORD` 应进入 secret manager、systemd `EnvironmentFile` 或部署平台密钥，不进入 git。
- 应用账号不需要 `DROP`、`CREATE USER`、`GRANT` 或全局权限。

## 4. 启动时让 Flyway 自动执行 V1

使用外部配置文件启动：

```bash
cp docs/deployment/application-dbflow-example.yml config/application-dbflow.yml

SPRING_PROFILES_ACTIVE=dbflow \
SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:./config/ \
java -jar target/refinex-dbflow-0.1.0-SNAPSHOT.jar
```

`config/application-dbflow.yml` 中的关键配置是：

```yaml
spring:
  datasource:
    url: ${DBFLOW_METADATA_JDBC_URL}
    username: ${DBFLOW_METADATA_USERNAME:dbflow_meta}
    password: ${DBFLOW_METADATA_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  flyway:
    locations: classpath:db/migration
  jpa:
    hibernate:
      ddl-auto: validate
```

启动成功后，metadata database 中应出现：

```text
flyway_schema_history
dbf_users
dbf_projects
dbf_environments
dbf_api_tokens
dbf_user_env_grants
dbf_confirmation_challenges
dbf_audit_events
```

验证命令：

```bash
mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$DBFLOW_METADATA_USERNAME" -p"$DBFLOW_METADATA_PASSWORD" \
  -D "$DBFLOW_METADATA_DATABASE" \
  -e "SHOW TABLES;"
```

## 5. 与 Target Databases 的区别

metadata database 存储 DBFlow 自己的控制面数据：

- 管理员和员工用户。
- MCP Token hash/prefix/status。
- project/env 元数据和授权。
- TRUNCATE confirmation。
- audit events。
- Flyway schema history。

target databases 是 AI 通过 DBFlow 被授权访问的业务库：

- 配在 `dbflow.projects[*].environments[*].jdbc-url`。
- 由 `HikariDataSourceRegistry` 创建独立 pool。
- 只在 `dbflow_inspect_schema`、`dbflow_explain_sql`、`dbflow_execute_sql` 等工具路径使用。
- 不应该出现 `dbf_*` 元数据表。

如果在业务库里看到了 `dbf_*` 表，通常说明 `spring.datasource.url` 错指到了业务库，应立即停止实例并修正配置。
