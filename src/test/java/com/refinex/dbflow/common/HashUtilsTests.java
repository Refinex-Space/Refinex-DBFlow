package com.refinex.dbflow.common;

import com.refinex.dbflow.common.util.HashUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hash 工具测试。
 *
 * @author refinex
 */
class HashUtilsTests {

    /**
     * 验证 SHA-256 十六进制摘要稳定且使用小写输出。
     */
    @Test
    void sha256HexShouldReturnStableLowercaseDigest() {
        String digest = HashUtils.sha256Hex("dbflow");

        assertThat(digest).isEqualTo("c4879ccb1454d19162c120bbfcb6d7c9c597bfd07612021ead6e4019435af020");
    }
}
