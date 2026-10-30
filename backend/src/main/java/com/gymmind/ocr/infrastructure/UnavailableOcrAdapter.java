package com.gymmind.ocr.infrastructure;

import com.gymmind.ocr.domain.OcrGateway;
import com.gymmind.ocr.domain.OcrRequest;
import com.gymmind.ocr.domain.OcrResult;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;

/** 降级适配器：功能关闭或未配置 Key 时装配，任何识别请求都返回依赖不可用 */
public class UnavailableOcrAdapter implements OcrGateway {

    private final String reason;

    public UnavailableOcrAdapter(String reason) {
        this.reason = reason == null || reason.isBlank() ? "OCR 服务不可用" : reason;
    }

    @Override
    public boolean supports(String contentType) {
        // 声明支持所有类型，让失败发生在统一的 recognize 降级文案上
        return true;
    }

    @Override
    public OcrResult recognize(OcrRequest request) {
        throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE, reason);
    }
}
