package com.gymmind.ocr.infrastructure;

import com.gymmind.ai.application.ModelUsage;
import com.gymmind.ocr.domain.OcrPage;
import com.gymmind.ocr.domain.OcrResult;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** DashScope OCR 适配器单测：响应解析、用量计量、类型支持与降级适配器行为 */
class DashScopeOcrAdapterTest {

    private static final String RESPONSE = """
            {
              "id": "req-123",
              "choices": [{"message": {"content": "# 会员体测单\\n\\n| 项目 | 数值 |\\n|---|---|\\n| 体重 | 75kg |"}}],
              "usage": {"input_tokens": 1200, "output_tokens": 80, "total_tokens": 1280}
            }
            """;

    private DashScopeOcrAdapter newAdapter() {
        // 解析类单测不发起真实 HTTP 请求，仅验证解析与类型判断
        return new DashScopeOcrAdapter(RestClient.builder(), "http://localhost:1", "test-key",
                5000, "qwen3.5-ocr", "qwen3.8-flash");
    }

    @Test
    void 解析响应返回Markdown与用量() {
        OcrResult result = newAdapter().parseResponse(RESPONSE, "qwen3.5-ocr");
        assertTrue(result.markdown().startsWith("# 会员体测单"));
        assertEquals(1, result.pages().size());
        assertEquals("dashscope", result.provider());
        assertEquals("req-123", result.providerRequestId());
        assertEquals(1200, result.usage().inputTokens());
        assertEquals(1280, result.usage().totalTokens());
        assertTrue(result.usage().available());
    }

    @Test
    void 解析异常响应返回用量不可用() {
        OcrResult result = newAdapter().parseResponse("{\"choices\": []}", "qwen3.5-ocr");
        assertEquals("", result.markdown());
        assertFalse(result.usage().available());
    }

    @Test
    void 支持的图片类型判断() {
        assertTrue(newAdapter().supports("image/png"));
        assertTrue(newAdapter().supports("IMAGE/JPEG"));
        assertFalse(newAdapter().supports("image/gif"));
        assertFalse(newAdapter().supports(null));
    }

    @Test
    void 未配置Key时识别抛依赖不可用() {
        DashScopeOcrAdapter noKey = new DashScopeOcrAdapter(RestClient.builder(), "http://localhost:1", "",
                5000, "qwen3.5-ocr", "qwen3.8-flash");
        BusinessException ex = assertThrows(BusinessException.class, () -> noKey.recognize(
                new com.gymmind.ocr.domain.OcrRequest("a.png", "image/png", new byte[]{1},
                        com.gymmind.ocr.domain.OcrOptions.defaults())));
        assertEquals(ErrorCode.DEPENDENCY_UNAVAILABLE, ex.errorCode());
    }

    @Test
    void 不支持的类型直接校验失败() {
        DashScopeOcrAdapter adapter = new DashScopeOcrAdapter(RestClient.builder(), "http://localhost:1", "k",
                5000, "qwen3.5-ocr", "qwen3.8-flash");
        BusinessException ex = assertThrows(BusinessException.class, () -> adapter.recognize(
                new com.gymmind.ocr.domain.OcrRequest("a.gif", "image/gif", new byte[]{1},
                        com.gymmind.ocr.domain.OcrOptions.defaults())));
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.errorCode());
    }

    @Test
    void 单页结果包含识别文本() {
        OcrResult result = newAdapter().parseResponse(RESPONSE, "qwen3.5-ocr");
        OcrPage page = result.pages().get(0);
        assertEquals(1, page.pageNumber());
        assertTrue(page.text().contains("体重"));
    }

    @Test
    void 用量携带实际使用的模型名() {
        // 回退场景：备用模型产生的用量应记录备用模型名，便于分池对账
        OcrResult result = newAdapter().parseResponse(RESPONSE, "qwen3.8-flash");
        assertEquals("qwen3.8-flash", result.usage().model());
        assertEquals(ModelUsage.unavailable("x", "y", "z").available(), false);
    }
}
