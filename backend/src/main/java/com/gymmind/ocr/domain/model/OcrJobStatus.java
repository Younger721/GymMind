package com.gymmind.ocr.domain.model;

/** OCR 任务状态机：PENDING（已上传）→ RUNNING（识别中）→ SUCCEEDED / FAILED */
public enum OcrJobStatus {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED
}
