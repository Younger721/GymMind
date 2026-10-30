package com.gymmind.ocr.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymmind.ai.application.ModelUsage;
import com.gymmind.ai.infrastructure.bailian.BailianFailureMessages;
import com.gymmind.ai.infrastructure.bailian.DashScopeProperties;
import com.gymmind.ocr.domain.OcrGateway;
import com.gymmind.ocr.domain.OcrPage;
import com.gymmind.ocr.domain.OcrRequest;
import com.gymmind.ocr.domain.OcrResult;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DashScope（阿里百炼）OpenAI 兼容模式 OCR 适配器。
 * <p>默认模型 {@code qwen3.5-ocr}（专用 OCR），其免费额度用尽/限流（403/429）时
 * 自动回退 {@code qwen3.8-flash} 一次——两个模型免费额度池独立，形成双份额度接力。
 */
public class DashScopeOcrAdapter implements OcrGateway {

    private static final Logger log = LoggerFactory.getLogger(DashScopeOcrAdapter.class);
    /** 支持的图片内容类型（与上传魔数校验双重保险） */
    private static final Set<String> SUPPORTED_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/bmp");
    private static final String PROMPT = "请识别图片中的全部文字内容，以 Markdown 格式输出：保持原有标题层级与列表结构；"
            + "表格输出为 Markdown 表格；图片中没有的文字不要编造；无法识别的区域用空字符串占位，不要输出解释性文字。";

    private final RestClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;
    private final String primaryModel;
    private final String fallbackModel;

    /** 生产装配：从配置取 Key、模型与超时 */
    public DashScopeOcrAdapter(RestClient.Builder builder, DashScopeProperties dashScope,
                               long timeoutMs, String primaryModel, String fallbackModel) {
        this(builder, dashScope.baseUrlForDashScope(), dashScope.apiKeyForDashScopeOnly(),
                timeoutMs, primaryModel, fallbackModel);
    }

    /** 测试装配：显式传入全部依赖 */
    DashScopeOcrAdapter(RestClient.Builder builder, String baseUrl, String apiKey,
                        long timeoutMs, String primaryModel, String fallbackModel) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5)).build());
        // OCR 单图识别耗时高于普通对话，超时从配置读取（默认 60s）
        requestFactory.setReadTimeout(Duration.ofMillis(Math.max(1000, timeoutMs)));
        this.client = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.primaryModel = normalize(primaryModel, "qwen3.5-ocr");
        this.fallbackModel = normalize(fallbackModel, "qwen3.8-flash");
    }

    @Override
    public boolean supports(String contentType) {
        return contentType != null && SUPPORTED_TYPES.contains(contentType.trim().toLowerCase());
    }

    @Override
    public OcrResult recognize(OcrRequest request) {
        if (apiKey.isBlank()) {
            throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE, "OCR 服务未配置 API Key");
        }
        if (!supports(request.contentType())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "不支持的图片类型: " + request.contentType());
        }
        long started = System.currentTimeMillis();
        try {
            OcrResult result = invokeChatCompletion(request, primaryModel);
            log.info("OCR 识别成功: model={}, 耗时={}ms", primaryModel, elapsed(started));
            return result;
        } catch (QuotaExhaustedOrRateLimited failure) {
            // 主模型免费额度用尽或限流：回退备用模型一次（独立额度池）
            log.warn("OCR 主模型不可用（{}），回退备用模型 {}", failure.getMessage(), fallbackModel);
            OcrResult result = invokeChatCompletion(request, fallbackModel);
            log.info("OCR 识别成功（备用模型）: model={}, 耗时={}ms", fallbackModel, elapsed(started));
            return result;
        }
    }

    /** 单模型调用：把图片编码为 base64 data URL 放入 OpenAI 兼容消息体 */
    private OcrResult invokeChatCompletion(OcrRequest request, String model) {
        String dataUrl = "data:" + request.contentType() + ";base64,"
                + Base64.getEncoder().encodeToString(request.content());
        Map<String, Object> imagePart = Map.of(
                "type", "image_url",
                "image_url", Map.of("url", dataUrl));
        Map<String, Object> textPart = Map.of("type", "text", "text", PROMPT);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0);
        body.put("messages", List.of(Map.of("role", "user", "content", List.of(imagePart, textPart))));
        String raw;
        try {
            raw = client.post().uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body).retrieve().body(String.class);
        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.TooManyRequests quota) {
            // 免费额度用完即停（403 AllocationQuota.FreeTierOnly）或限流（429）→ 触发备用模型回退
            throw new QuotaExhaustedOrRateLimited(quota);
        } catch (RuntimeException failure) {
            throw providerUnavailable(failure);
        }
        return parseResponse(raw, model);
    }

    /** 解析 OpenAI 兼容响应：content 为 Markdown，单图为单页结果（包私有便于单测） */
    OcrResult parseResponse(String raw, String model) {
        JsonNode root;
        try {
            root = mapper.readTree(raw);
        } catch (Exception failure) {
            throw providerUnavailable(failure);
        }
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        ModelUsage usage = parseUsage(root, model);
        return new OcrResult(
                List.of(new OcrPage(1, content, null)),
                content,
                "dashscope",
                model,
                root.path("id").asText(null),
                usage);
    }

    /** 解析 usage 计量段（结构与 BailianUsageParser 一致，但 OCR 需要携带实际使用的模型名） */
    private static ModelUsage parseUsage(JsonNode root, String model) {
        JsonNode usage = root.path("usage");
        if (!usage.isObject()) {
            return ModelUsage.unavailable("dashscope", model, root.path("id").asText(null));
        }
        int input = usage.path("input_tokens").asInt(-1);
        int output = usage.path("output_tokens").asInt(-1);
        if (input < 0 && usage.has("prompt_tokens")) {
            input = usage.path("prompt_tokens").asInt(-1);
        }
        if (output < 0 && usage.has("completion_tokens")) {
            output = usage.path("completion_tokens").asInt(-1);
        }
        Integer in = input >= 0 ? input : null;
        Integer out = output >= 0 ? output : null;
        Integer total = usage.path("total_tokens").asInt(-1);
        Integer totalTokens = total >= 0 ? total : (in != null || out != null ? sum(in, out) : null);
        return new ModelUsage("dashscope", model, in, out, totalTokens, root.path("id").asText(null), true);
    }

    private static BusinessException providerUnavailable(Throwable failure) {
        return new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                BailianFailureMessages.dependencyUnavailableMessage(failure));
    }

    private static Integer sum(Integer a, Integer b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a + b;
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static long elapsed(long started) {
        return Math.max(0, System.currentTimeMillis() - started);
    }

    /** 内部信号：主模型额度用尽/限流，可回退备用模型 */
    private static final class QuotaExhaustedOrRateLimited extends RuntimeException {
        private QuotaExhaustedOrRateLimited(Throwable cause) {
            super(cause.getMessage(), cause);
        }
    }
}
