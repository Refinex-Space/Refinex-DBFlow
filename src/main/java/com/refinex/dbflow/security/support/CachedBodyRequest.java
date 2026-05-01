package com.refinex.dbflow.security.support;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 可重复读取的请求体包装器。
 *
 * @author refinex
 */
public class CachedBodyRequest extends HttpServletRequestWrapper {

    /**
     * 缓存请求体。
     */
    private final byte[] body;

    /**
     * 创建缓存请求体包装器。
     *
     * @param request 原始 HTTP 请求
     * @param body    缓存请求体
     */
    public CachedBodyRequest(HttpServletRequest request, byte[] body) {
        super(request);
        this.body = body.clone();
    }

    /**
     * 返回缓存请求体输入流。
     *
     * @return Servlet 输入流
     */
    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream input = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return input.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // 缓存请求体已在过滤器中同步读取完成。
            }

            @Override
            public int read() {
                return input.read();
            }
        };
    }

    /**
     * 返回缓存请求体字符读取器。
     *
     * @return 字符读取器
     */
    @Override
    public BufferedReader getReader() {
        Charset charset = getCharacterEncoding() == null
                ? StandardCharsets.UTF_8
                : Charset.forName(getCharacterEncoding());
        return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }
}
