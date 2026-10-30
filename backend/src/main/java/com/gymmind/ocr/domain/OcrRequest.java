package com.gymmind.ocr.domain;

/** OCR 识别请求：图片二进制只在此记录中出现，编排层以 uploadId 引用 */
public record OcrRequest(String fileName, String contentType, byte[] content, OcrOptions options) {

    public OcrRequest {
        if (options == null) {
            options = OcrOptions.defaults();
        }
    }
}
