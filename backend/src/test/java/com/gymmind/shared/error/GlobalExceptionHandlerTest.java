package com.gymmind.shared.error;

import com.gymmind.shared.api.TraceIdFilter;
import com.gymmind.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new TraceIdFilter())
                .build();
    }

    @Test
    void validationErrorsUseStableEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/test-validation")
                        .header(TraceIdFilter.HEADER_NAME, "validation_trace_1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(TraceIdFilter.HEADER_NAME, "validation_trace_1"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("参数校验失败"))
                .andExpect(jsonPath("$.traceId").value("validation_trace_1"));
    }

    @Test
    void businessErrorsUseMappedStatusWithoutEchoingExceptionText() throws Exception {
        mockMvc.perform(post("/api/v1/test-conflict")
                        .header(TraceIdFilter.HEADER_NAME, "business_trace_1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("资源冲突"))
                .andExpect(jsonPath("$.traceId").value("business_trace_1"))
                .andExpect(contentDoesNotContain("duplicate-key-password=secret"));
    }

    @Test
    void unexpectedErrorsReturnSanitizedInternalErrorWithSafeDiagnostics(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/api/v1/test-failure")
                        .header(TraceIdFilter.HEADER_NAME, "failure_trace_01"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("服务器内部错误"))
                .andExpect(jsonPath("$.traceId").value("failure_trace_01"))
                .andExpect(contentDoesNotContain("SELECT token, password"))
                .andExpect(contentDoesNotContain("credentials"))
                .andExpect(contentDoesNotContain("stackTrace"))
                .andExpect(contentDoesNotContain("exception"))
                .andExpect(contentDoesNotContain("cause"));

        assertThat(output.getOut()).contains("traceId=failure_trace_01")
                .contains("java.lang.IllegalStateException")
                .contains("java.lang.IllegalArgumentException")
                .contains("GlobalExceptionHandlerTest$TestController.fail")
                .doesNotContain("SELECT token, password")
                .doesNotContain("cause password=hidden-secret");
    }

    @Test
    void malformedJsonUsesValidationEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/test-validation")
                        .header(TraceIdFilter.HEADER_NAME, "malformed_trace_1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("参数校验失败"))
                .andExpect(jsonPath("$.traceId").value("malformed_trace_1"));
    }

    @Test
    void asyncResponsesRetainTraceIdInHeaderAndEnvelope() throws Exception {
        MvcResult started = mockMvc.perform(post("/api/v1/test-async")
                        .header(TraceIdFilter.HEADER_NAME, "async_trace_123"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceIdFilter.HEADER_NAME, "async_trace_123"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.traceId").value("async_trace_123"));
    }

    private static org.springframework.test.web.servlet.ResultMatcher contentDoesNotContain(String value) {
        return org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(not(containsString(value)));
    }

    @RestController
    static class TestController {
        @PostMapping("/api/v1/test-validation")
        String validate(@Valid @RequestBody TestRequest request) {
            return request.name();
        }

        @PostMapping("/api/v1/test-conflict")
        void conflict() {
            throw new BusinessException(ErrorCode.CONFLICT, "duplicate-key-password=secret");
        }

        @PostMapping("/api/v1/test-failure")
        void fail() {
            IllegalArgumentException cause = new IllegalArgumentException("cause password=hidden-secret");
            throw new IllegalStateException(
                    "SELECT token, password FROM credentials WHERE secret='hidden'",
                    cause
            );
        }

        @PostMapping("/api/v1/test-async")
        Callable<ApiResponse<String>> async() {
            return () -> ApiResponse.success("ready");
        }
    }

    record TestRequest(@NotBlank String name) {
    }
}
