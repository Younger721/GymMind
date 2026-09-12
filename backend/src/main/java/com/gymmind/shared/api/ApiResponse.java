package com.gymmind.shared.api;

import org.slf4j.MDC;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        String traceId
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "OK", "Success", data, currentTraceId());
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, code, message, null, currentTraceId());
    }

    private static String currentTraceId() {
        return MDC.get(TraceIdFilter.MDC_KEY);
    }
}
