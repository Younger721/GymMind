package com.gymmind.ocr.infrastructure;

import com.gymmind.ai.infrastructure.bailian.DashScopeProperties;
import com.gymmind.ocr.application.OcrProperties;
import com.gymmind.ocr.domain.OcrGateway;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** OCR 模块装配：按配置选择 DashScope 适配器或降级适配器 */
@Configuration
@EnableConfigurationProperties(OcrProperties.class)
public class OcrConfiguration {

    /** 按配置装配网关：功能关闭或未配置 Key 时装配降级适配器，识别请求返回明确文案 */
    @Bean
    public OcrGateway ocrGateway(RestClient.Builder builder,
                                 DashScopeProperties dashScope,
                                 OcrProperties ocr) {
        // 功能未开启：降级适配器，识别请求返回明确的"未启用"文案
        if (!ocr.isEnabled()) {
            return new UnavailableOcrAdapter("OCR 功能未启用（gymmind.ocr.enabled=false）");
        }
        // 未配置百炼 Key：降级适配器，提示补配置
        if (dashScope.apiKeyForDashScopeOnly().isBlank()) {
            return new UnavailableOcrAdapter("OCR 服务未配置（缺少 DASHSCOPE_API_KEY）");
        }
        return new DashScopeOcrAdapter(builder, dashScope, ocr.getTimeoutMs(),
                dashScope.getOcrModel(), dashScope.getOcrFallbackModel());
    }
}
