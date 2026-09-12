package com.gymmind.shared.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";
    public static final String REQUEST_ATTRIBUTE = TraceIdFilter.class.getName() + ".traceId";

    private static final Pattern ACCEPTED_TRACE_ID = Pattern.compile("[A-Za-z0-9_-]{8,64}");
    private static final Object CALLABLE_INTERCEPTOR_KEY = TraceIdFilter.class.getName() + ".callable";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        request.setAttribute(REQUEST_ATTRIBUTE, traceId);
        response.setHeader(HEADER_NAME, traceId);
        registerCallableInterceptor(request, traceId);
        MDC.put(MDC_KEY, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    private String resolveTraceId(HttpServletRequest request) {
        Object existing = request.getAttribute(REQUEST_ATTRIBUTE);
        if (existing instanceof String traceId) {
            return traceId;
        }
        String candidate = request.getHeader(HEADER_NAME);
        if (candidate != null && ACCEPTED_TRACE_ID.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }

    private void registerCallableInterceptor(HttpServletRequest request, String traceId) {
        var asyncManager = WebAsyncUtils.getAsyncManager(request);
        if (asyncManager.getCallableInterceptor(CALLABLE_INTERCEPTOR_KEY) == null) {
            asyncManager.registerCallableInterceptor(
                    CALLABLE_INTERCEPTOR_KEY,
                    new TraceIdCallableInterceptor(traceId)
            );
        }
    }

    private record TraceIdCallableInterceptor(String traceId) implements CallableProcessingInterceptor {
        @Override
        public <T> void preProcess(NativeWebRequest request, Callable<T> task) {
            MDC.put(MDC_KEY, traceId);
        }

        @Override
        public <T> void postProcess(NativeWebRequest request, Callable<T> task, Object concurrentResult) {
            MDC.remove(MDC_KEY);
        }

        @Override
        public <T> void afterCompletion(NativeWebRequest request, Callable<T> task) {
            MDC.remove(MDC_KEY);
        }
    }
}
