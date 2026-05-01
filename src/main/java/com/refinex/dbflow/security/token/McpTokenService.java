package com.refinex.dbflow.security.token;

import com.refinex.dbflow.access.entity.DbfApiToken;
import com.refinex.dbflow.access.repository.DbfApiTokenRepository;
import com.refinex.dbflow.common.DbflowException;
import com.refinex.dbflow.common.ErrorCode;
import com.refinex.dbflow.security.properties.McpTokenProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * MCP Token 生命周期服务，负责生成、hash、吊销、重新颁发和校验。
 *
 * @author refinex
 */
@Service
public class McpTokenService {

    /**
     * Token 明文前缀。
     */
    private static final String TOKEN_PREFIX = "dbf_";

    /**
     * Token 随机字节长度。
     */
    private static final int TOKEN_RANDOM_BYTES = 32;

    /**
     * Token 展示前缀长度。
     */
    private static final int TOKEN_DISPLAY_PREFIX_LENGTH = 16;

    /**
     * HMAC 算法名称。
     */
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * API Token repository。
     */
    private final DbfApiTokenRepository apiTokenRepository;

    /**
     * MCP Token 配置。
     */
    private final McpTokenProperties properties;

    /**
     * 安全随机数生成器。
     */
    private final SecureRandom secureRandom;

    /**
     * 创建 MCP Token 生命周期服务。
     *
     * @param apiTokenRepository API Token repository
     * @param properties         MCP Token 配置
     */
    @Autowired
    public McpTokenService(DbfApiTokenRepository apiTokenRepository, McpTokenProperties properties) {
        this(apiTokenRepository, properties, new SecureRandom());
    }

    /**
     * 创建 MCP Token 生命周期服务。
     *
     * @param apiTokenRepository API Token repository
     * @param properties         MCP Token 配置
     * @param secureRandom       安全随机数生成器
     */
    McpTokenService(
            DbfApiTokenRepository apiTokenRepository,
            McpTokenProperties properties,
            SecureRandom secureRandom
    ) {
        this.apiTokenRepository = apiTokenRepository;
        this.properties = properties;
        this.secureRandom = secureRandom;
    }

    /**
     * 为用户颁发唯一 active MCP Token。
     *
     * @param userId    用户主键
     * @param expiresAt 过期时间
     * @return Token 颁发结果，包含一次性明文 Token
     */
    @Transactional
    public McpTokenIssueResult issueToken(Long userId, Instant expiresAt) {
        apiTokenRepository.findByUserIdAndStatus(userId, "ACTIVE").ifPresent(token -> {
            throw new DbflowException(ErrorCode.INVALID_REQUEST, "用户已有 active MCP Token");
        });
        String plaintextToken = generatePlaintextToken();
        DbfApiToken token = apiTokenRepository.save(DbfApiToken.active(
                userId,
                hashToken(plaintextToken),
                tokenPrefix(plaintextToken),
                expiresAt
        ));
        return new McpTokenIssueResult(
                token.getId(),
                token.getUserId(),
                plaintextToken,
                token.getTokenPrefix(),
                token.getExpiresAt()
        );
    }

    /**
     * 吊销用户当前 active MCP Token。
     *
     * @param userId    用户主键
     * @param revokedAt 吊销时间
     * @return 吊销后的 Token 元数据
     */
    @Transactional
    public DbfApiToken revokeActiveToken(Long userId, Instant revokedAt) {
        DbfApiToken token = apiTokenRepository.findByUserIdAndStatus(userId, "ACTIVE")
                .orElseThrow(() -> new DbflowException(ErrorCode.INVALID_REQUEST, "active MCP Token 不存在"));
        token.revoke(revokedAt);
        return apiTokenRepository.save(token);
    }

    /**
     * 校验 MCP Token 并更新最近使用时间。
     *
     * @param plaintextToken 明文 Token
     * @param usedAt         使用时间
     * @return 校验成功结果；失败时为空
     */
    @Transactional
    public Optional<McpTokenValidationResult> validateToken(String plaintextToken, Instant usedAt) {
        if (plaintextToken == null || plaintextToken.isBlank()) {
            return Optional.empty();
        }
        String candidateHash = hashToken(plaintextToken);
        return apiTokenRepository.findByTokenHash(candidateHash)
                .filter(token -> isActiveAndNotExpired(token, usedAt))
                .filter(token -> hashEquals(candidateHash, token.getTokenHash()))
                .map(token -> markUsed(token, usedAt));
    }

    /**
     * 生成明文 Token。
     *
     * @return 明文 Token
     */
    private String generatePlaintextToken() {
        byte[] tokenBytes = new byte[TOKEN_RANDOM_BYTES];
        secureRandom.nextBytes(tokenBytes);
        return TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * 计算 Token hash。
     *
     * @param plaintextToken 明文 Token
     * @return Token hash
     */
    private String hashToken(String plaintextToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(requiredPepper().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(plaintextToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("MCP Token hash 计算失败", exception);
        }
    }

    /**
     * 读取必需的 Token pepper。
     *
     * @return Token pepper
     */
    private String requiredPepper() {
        if (properties.getPepper() == null || properties.getPepper().isBlank()) {
            throw new IllegalStateException("dbflow.security.mcp-token.pepper 不能为空");
        }
        return properties.getPepper();
    }

    /**
     * 生成可展示 Token 前缀。
     *
     * @param plaintextToken 明文 Token
     * @return 可展示 Token 前缀
     */
    private String tokenPrefix(String plaintextToken) {
        return plaintextToken.substring(0, Math.min(TOKEN_DISPLAY_PREFIX_LENGTH, plaintextToken.length()));
    }

    /**
     * 判断 Token 是否 active 且未过期。
     *
     * @param token Token 元数据
     * @param now   当前时间
     * @return 可用时返回 true
     */
    private boolean isActiveAndNotExpired(DbfApiToken token, Instant now) {
        return "ACTIVE".equals(token.getStatus()) && (token.getExpiresAt() == null || token.getExpiresAt().isAfter(now));
    }

    /**
     * 使用常量时间比较 hash。
     *
     * @param candidateHash 候选 hash
     * @param storedHash    存储 hash
     * @return 相等时返回 true
     */
    private boolean hashEquals(String candidateHash, String storedHash) {
        return MessageDigest.isEqual(
                candidateHash.getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * 更新最近使用时间并返回校验结果。
     *
     * @param token  Token 元数据
     * @param usedAt 使用时间
     * @return 校验成功结果
     */
    private McpTokenValidationResult markUsed(DbfApiToken token, Instant usedAt) {
        token.markUsed(usedAt);
        DbfApiToken savedToken = apiTokenRepository.save(token);
        return new McpTokenValidationResult(
                savedToken.getId(),
                savedToken.getUserId(),
                savedToken.getTokenPrefix(),
                savedToken.getLastUsedAt()
        );
    }
}
