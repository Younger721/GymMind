package com.gymmind.shared.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void successResponseUsesCurrentTraceIdAndStableEnvelope() throws Exception {
        MDC.put(TraceIdFilter.MDC_KEY, "request_trace_123");

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(ApiResponse.success("ready")));

        assertThat(json.get("success").asBoolean()).isTrue();
        assertThat(json.get("code").asText()).isEqualTo("OK");
        assertThat(json.get("message").asText()).isEqualTo("操作成功");
        assertThat(json.get("data").asText()).isEqualTo("ready");
        assertThat(json.get("traceId").asText()).isEqualTo("request_trace_123");
    }

    @Test
    void pageResponseExposesPaginationMetadata() throws Exception {
        PageResponse<String> page = new PageResponse<>(java.util.List.of("first", "second"), 1, 2, 7, 4);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(ApiResponse.success(page)));

        assertThat(json.at("/data/content")).hasSize(2);
        assertThat(json.at("/data/page").asInt()).isEqualTo(1);
        assertThat(json.at("/data/size").asInt()).isEqualTo(2);
        assertThat(json.at("/data/totalElements").asLong()).isEqualTo(7);
        assertThat(json.at("/data/totalPages").asInt()).isEqualTo(4);
    }
}
