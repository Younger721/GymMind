package com.gymmind.ai.infrastructure;

import com.gymmind.ai.infrastructure.bailian.BailianEmbeddingAdapter;
import com.gymmind.ai.infrastructure.bailian.DashScopeProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BailianEmbeddingAdapterTest {

    private static DashScopeProperties dashScopeWithKey() {
        DashScopeProperties properties = new DashScopeProperties();
        properties.setApiKey("sk-test");
        return properties;
    }
    @Test
    void sendsEmbeddingTextsAsOpenAiCompatibleInputArray() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String vector = IntStream.range(0, 1024)
                .mapToObj(index -> "0.1")
                .collect(Collectors.joining(",", "[", "]"));

        server.expect(requestTo("https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"model\":\"qwen3.7-text-embedding\",\"input\":[\"hello\"]}"))
                .andRespond(withSuccess("{\"data\":[{\"embedding\":" + vector + "}]}", MediaType.APPLICATION_JSON));

        var result = new BailianEmbeddingAdapter(builder, dashScopeWithKey()).embed(java.util.List.of("hello"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).hasSize(1024);
        server.verify();
    }

    @Test
    void splitsBatchesLargerThanDashscopeLimitAndPreservesOrder() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String vector = IntStream.range(0, 1024)
                .mapToObj(index -> "0.1")
                .collect(Collectors.joining(",", "[", "]"));
        var firstBatch = IntStream.range(0, 20).mapToObj(i -> "doc-" + i).toList();
        var secondBatch = java.util.List.of("doc-20");
        String firstRequest = "{\"model\":\"qwen3.7-text-embedding\",\"input\":["
                + firstBatch.stream().map(value -> "\"" + value + "\"").collect(Collectors.joining(","))
                + "]}";
        String secondRequest = "{\"model\":\"qwen3.7-text-embedding\",\"input\":[\"doc-20\"]}";

        server.expect(requestTo("https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings"))
                .andExpect(content().json(firstRequest))
                .andRespond(withSuccess("{\"data\":[" + firstBatch.stream().map(value -> "{\"embedding\":" + vector + "}").collect(Collectors.joining(",")) + "]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings"))
                .andExpect(content().json(secondRequest))
                .andRespond(withSuccess("{\"data\":[{\"embedding\":" + vector + "}]}", MediaType.APPLICATION_JSON));

        var result = new BailianEmbeddingAdapter(builder, dashScopeWithKey()).embed(
                IntStream.range(0, 21).mapToObj(i -> "doc-" + i).toList());

        assertThat(result).hasSize(21);
        assertThat(result).allSatisfy(item -> assertThat(item).hasSize(1024));
        server.verify();
    }
}
