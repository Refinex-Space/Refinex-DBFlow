# Java Development Standards

本文档定义 Refinex-DBFlow 后端开发规范。新增或修改 Java、Maven、YAML、XML 配置时，默认遵守本文档；如果后续接入 Checkstyle、SpotBugs、PMD、SonarQube 或 CI，本文件是规则配置的人工基线。

## 基本原则

- 代码风格遵循阿里巴巴 Java 开发规范，并以安全、正确性、可维护性为优先级。
- 所有注释使用中文；技术名词、类名、方法名、配置键、协议名保留英文原文。
- 注释必须解释业务意图、边界、风险或配置目的，不写“获取某字段”这类无信息量注释。
- 业务代码不得吞异常、不得记录敏感信息、不得输出 Token 明文、密码、连接串密码或完整敏感查询结果。
- 新增公共能力前先写可验证测试；不能测试时，必须在执行计划中写明替代验证证据。

## Java 注释规范

所有后端类必须有标准类注释，模板如下：

```java
/**
 * <description>
 *
 * @author refinex
 */
```

- `<description>` 用一句中文说明类职责，不写泛化描述。
- 方法注释必须覆盖所有参数；有返回值时写 `@return`，可能抛出业务异常时写 `@throws`。
- 属性注释必须说明字段含义、单位、脱敏要求、默认值或约束之一；实体、配置属性、DTO、枚举字段必须逐项说明。
- 测试类和测试方法也要注释测试目标，避免测试意图只能从实现里猜。
- 包级边界稳定后，可以增加 `package-info.java` 说明包职责和依赖方向。

方法注释示例：

```java
/**
 * 校验用户是否具备目标项目环境的访问权限。
 *
 * @param userId 用户标识
 * @param projectKey 项目标识
 * @param environmentKey 环境标识
 * @return 具备访问权限时返回 true
 */
boolean hasAccess(Long userId, String projectKey, String environmentKey) {
    return false;
}
```

## Maven 注释规范

- `pom.xml` 中每个 BOM、直接依赖、插件都必须有中文注释，说明用途和阶段边界。
- 不在业务依赖上省略来源说明；新增依赖前确认是否已经由 Spring Boot、Spring AI、Spring Cloud 或 Spring Cloud Alibaba BOM 管理。
- 不引入未使用依赖；P01 阶段只保留骨架必需依赖。
- 依赖版本优先由 BOM 管理；确需显式版本时必须说明原因。

## 配置注释规范

- YAML、properties、XML 配置必须用中文注释说明配置意图、默认值影响和敏感信息边界。
- `logback-spring.xml` 中每个 appender、logger、root level 都必须有中文注释。
- 不把密钥、Token pepper、数据库密码、Nacos 凭据写入仓库；示例只能使用占位符。

## SonarQube 取向

在自动化规则接入前，人工遵守以下底线：

- 方法保持单一职责，避免深层嵌套和过高认知复杂度。
- 不复制粘贴业务逻辑；重复出现三次以上先提取有名字的私有方法或组件。
- 不新增未使用代码、未使用依赖、未使用配置。
- 日志输出使用参数化日志，不拼接敏感字符串。
- 对外输入必须明确校验边界；数据库、MCP、管理端入口不得信任客户端声明。

## 阶段性说明

当前仓库仍处于 P01 骨架阶段，尚未接入格式化器、静态扫描、SonarQube 或 CI。后续 P01.2 或质量专项应将本文档转化为可执行工具配置，并把命令写入 `docs/OBSERVABILITY.md`。
