package com.gymmind.ocr.application;

/** 上传结果视图：后续识别与入库只引用 uploadId（先上传后引用约束） */
public record OcrUploadView(Long uploadId, String fileName, String contentType, int sizeBytes) {
}
