package com.gymmind.shared.api;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void validClientTraceIdFlowsThroughRequestMdcAndResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.HEADER_NAME, "client_trace-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (filteredRequest, filteredResponse) -> {
            assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isEqualTo("client_trace-123");
            assertThat(filteredRequest.getAttribute(TraceIdFilter.REQUEST_ATTRIBUTE)).isEqualTo("client_trace-123");
            assertThat(((MockHttpServletResponse) filteredResponse).getHeader(TraceIdFilter.HEADER_NAME))
                    .isEqualTo("client_trace-123");
        });

        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void invalidClientTraceIdIsReplacedWithUuid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.HEADER_NAME, "bad trace containing spaces and password=secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (filteredRequest, filteredResponse) -> {
            String traceId = MDC.get(TraceIdFilter.MDC_KEY);
            assertThat(traceId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
            assertThat(filteredRequest.getAttribute(TraceIdFilter.REQUEST_ATTRIBUTE)).isEqualTo(traceId);
        });

        assertThat(response.getHeader(TraceIdFilter.HEADER_NAME)).isNotEqualTo("bad trace containing spaces and password=secret");
        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void onlyTraceIdsWithinLengthBoundariesAreReused() throws Exception {
        List<String> accepted = List.of("12345678", "a".repeat(64));
        List<String> rejected = List.of("1234567", "a".repeat(65), "trace.id", "追踪标识12345678");

        for (String candidate : accepted) {
            assertResolvedTraceId(candidate, candidate);
        }
        for (String candidate : rejected) {
            assertResolvedTraceIdIsUuid(candidate);
        }
    }

    @Test
    void threadContextIsClearedWhenDownstreamThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response, (filteredRequest, filteredResponse) -> {
            throw new ServletException("downstream failure");
        })).isInstanceOf(ServletException.class);

        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }

    private void assertResolvedTraceId(String candidate, String expected) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.HEADER_NAME, candidate);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (filteredRequest, filteredResponse) -> {
            assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isEqualTo(expected);
            assertThat(filteredRequest.getAttribute(TraceIdFilter.REQUEST_ATTRIBUTE)).isEqualTo(expected);
            assertThat(((MockHttpServletResponse) filteredResponse).getHeader(TraceIdFilter.HEADER_NAME))
                    .isEqualTo(expected);
        });
    }

    private void assertResolvedTraceIdIsUuid(String candidate) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.HEADER_NAME, candidate);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (filteredRequest, filteredResponse) -> {
            String resolved = MDC.get(TraceIdFilter.MDC_KEY);
            assertThat(resolved).matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
            assertThat(resolved).isEqualTo(filteredRequest.getAttribute(TraceIdFilter.REQUEST_ATTRIBUTE));
            assertThat(resolved).isEqualTo(((MockHttpServletResponse) filteredResponse)
                    .getHeader(TraceIdFilter.HEADER_NAME));
        });
    }
}
