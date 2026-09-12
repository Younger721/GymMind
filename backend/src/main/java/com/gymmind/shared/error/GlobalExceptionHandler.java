package com.gymmind.shared.error;

import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.api.TraceIdFilter;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final int MAX_CAUSE_DEPTH = 8;
    private static final int MAX_FRAMES_PER_CAUSE = 16;

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            HandlerMethodValidationException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleValidation(Exception exception) {
        return response(ErrorCode.VALIDATION_FAILED);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception) {
        return response(exception.errorCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        log.error("Unhandled request failure ({})", safeDiagnostics(exception));
        return response(ErrorCode.INTERNAL_ERROR);
    }

    private String safeDiagnostics(Throwable failure) {
        StringBuilder diagnostics = new StringBuilder("traceId=")
                .append(MDC.get(TraceIdFilter.MDC_KEY));
        Throwable current = failure;
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
            diagnostics.append(", ")
                    .append(depth == 0 ? "exceptionType=" : "causeType=")
                    .append(current.getClass().getName());
            StackTraceElement[] frames = current.getStackTrace();
            int frameCount = Math.min(frames.length, MAX_FRAMES_PER_CAUSE);
            for (int index = 0; index < frameCount; index++) {
                diagnostics.append(System.lineSeparator())
                        .append("  at ")
                        .append(frames[index]);
            }
            current = current.getCause();
        }
        return diagnostics.toString();
    }

    private ResponseEntity<ApiResponse<Void>> response(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.status())
                .body(ApiResponse.error(errorCode.name(), errorCode.message()));
    }
}
