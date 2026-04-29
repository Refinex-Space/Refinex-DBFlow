package com.refinex.dbflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Refinex-DBFlow Spring Boot 应用启动入口。
 *
 * @author refinex
 */
@SpringBootApplication
public class DbflowApplication {

    /**
     * 启动 Refinex-DBFlow 应用上下文。
     *
     * @param args 命令行启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DbflowApplication.class, args);
    }
}
