package com.gymmind.ocr.application;

import com.gymmind.shared.security.CurrentActor;

/** OCR 用例端口：上传引用 + 同步识别（M1） */
public interface OcrService {

    /** 上传图片到对象存储并登记任务，返回 uploadId（有效期见配置） */
    OcrUploadView upload(CurrentActor actor, String fileName, String contentType, byte[] content);

    /** 按上传 ID 同步识别，返回 Markdown 与用量 */
    OcrResultView recognize(CurrentActor actor, Long uploadId);
}
