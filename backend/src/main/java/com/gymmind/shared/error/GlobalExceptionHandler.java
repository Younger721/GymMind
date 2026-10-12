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
import org.springframework.validation.FieldError;
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
        if (exception instanceof MethodArgumentNotValidException ex) {
            return validationResponse(ex.getBindingResult().getFieldErrors().stream()
                    .map(this::formatFieldError).findFirst().orElse("请求参数不合法"));
        }
        if (exception instanceof BindException ex) {
            return validationResponse(ex.getBindingResult().getFieldErrors().stream()
                    .map(this::formatFieldError).findFirst().orElse("请求参数不合法"));
        }
        if (exception instanceof ConstraintViolationException ex) {
            return validationResponse(ex.getConstraintViolations().stream()
                    .map(violation -> violation.getPropertyPath() + "：" + violation.getMessage())
                    .findFirst().orElse("请求参数不合法"));
        }
        if (exception instanceof HttpMessageNotReadableException) {
            return validationResponse("请求体格式不正确，请检查 JSON 内容");
        }
        if (exception instanceof MissingServletRequestParameterException ex) {
            return validationResponse("缺少请求参数：" + ex.getParameterName());
        }
        if (exception instanceof MethodArgumentTypeMismatchException ex) {
            return validationResponse("请求参数“" + ex.getName() + "”格式不正确");
        }
        return validationResponse("请求参数不合法");
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + "：" + error.getDefaultMessage();
    }

    private ResponseEntity<ApiResponse<Void>> validationResponse(String message) {
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status())
                .body(ApiResponse.error(ErrorCode.VALIDATION_FAILED.name(), message));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception) {
        // 依赖类错误保留业务侧给出的可读说明（如百炼调用失败原因摘要），便于排查
        if (exception.errorCode() == ErrorCode.DEPENDENCY_UNAVAILABLE) {
            String detail = exception.getMessage();
            if (detail != null && !detail.isBlank() && !detail.equals(exception.errorCode().message())) {
                return ResponseEntity.status(exception.errorCode().status())
                        .body(ApiResponse.error(exception.errorCode().name(), detail));
            }
        }
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
