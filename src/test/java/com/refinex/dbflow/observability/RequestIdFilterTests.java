package com.refinex.dbflow.observability;

import com.refinex.dbflow.observability.filter.RequestIdFilter;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 请求标识过滤器测试。
 *
 * @author refinex
 */
class RequestIdFilterTests {

    /**
     * 验证调用方传入 request id 时沿用该标识。
     *
     * @throws ServletException Servlet 处理异常
     * @throws IOException IO 处理异常
     */
    @Test
    void shouldReuseIncomingRequestId() throws ServletException, IOException {
        RequestIdFilter filter = new RequestIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInChain = new AtomicReference<>();
        AtomicReference<String> traceIdInChain = new AtomicReference<>();
        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "req-123");
        request.addHeader(RequestIdFilter.TRACE_ID_HEADER, "trace-abc");

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            requestIdInChain.set(MDC.get(RequestIdFilter.MDC_KEY));
            traceIdInChain.set(MDC.get(RequestIdFilter.TRACE_MDC_KEY));
        });

        assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo("req-123");
        assertThat(response.getHeader(RequestIdFilter.TRACE_ID_HEADER)).isEqualTo("trace-abc");
        assertThat(requestIdInChain.get()).isEqualTo("req-123");
        assertThat(traceIdInChain.get()).isEqualTo("trace-abc");
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
        assertThat(MDC.get(RequestIdFilter.TRACE_MDC_KEY)).isNull();
    }

    /**
     * 验证调用方未传入 request id 时自动生成新标识。
     *
     * @throws ServletException Servlet 处理异常
     * @throws IOException IO 处理异常
     */
    @Test
    void shouldGenerateRequestIdWhenMissing() throws ServletException, IOException {
        RequestIdFilter filter = new RequestIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInChain = new AtomicReference<>();
        AtomicReference<String> traceIdInChain = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            requestIdInChain.set(MDC.get(RequestIdFilter.MDC_KEY));
            traceIdInChain.set(MDC.get(RequestIdFilter.TRACE_MDC_KEY));
        });

        assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isNotBlank();
        assertThat(response.getHeader(RequestIdFilter.TRACE_ID_HEADER)).isEqualTo(
                response.getHeader(RequestIdFilter.REQUEST_ID_HEADER));
        assertThat(requestIdInChain.get()).isEqualTo(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER));
        assertThat(traceIdInChain.get()).isEqualTo(response.getHeader(RequestIdFilter.TRACE_ID_HEADER));
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
        assertThat(MDC.get(RequestIdFilter.TRACE_MDC_KEY)).isNull();
    }
}
