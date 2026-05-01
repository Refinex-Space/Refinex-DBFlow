package com.refinex.dbflow.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hash 工具，统一提供非业务专属摘要算法。
 *
 * @author refinex
 */
public final class HashUtils {

    /**
     * SHA-256 算法名称。
     */
    private static final String SHA_256 = "SHA-256";

    /**
     * 工具类不允许实例化。
     */
    private HashUtils() {
    }

    /**
     * 计算 UTF-8 文本的 SHA-256 十六进制摘要。
     *
     * @param value 原始文本
     * @return 小写十六进制摘要
     */
    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", exception);
        }
    }
}
