package com.refinex.dbflow.executor;

/**
 * schema inspect 请求。
 *
 * @param requestId      请求标识
 * @param userId         用户主键
 * @param tokenId        Token 主键
 * @param tokenPrefix    Token 展示前缀，禁止传入明文 Token
 * @param projectKey     项目标识
 * @param environmentKey 环境标识
 * @param schema         schema 过滤；为空时使用目标连接当前 catalog
 * @param table          table 过滤；为空时返回 schema 级摘要
 * @param maxItems       每类元数据最大返回条目数
 * @author refinex
 */
public record SchemaInspectRequest(
        String requestId,
        Long userId,
        Long tokenId,
        String tokenPrefix,
        String projectKey,
        String environmentKey,
        String schema,
        String table,
        int maxItems
) {
}
