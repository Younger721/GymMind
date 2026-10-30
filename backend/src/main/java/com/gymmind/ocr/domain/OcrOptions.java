package com.gymmind.ocr.domain;

/** OCR 识别选项：语言提示与输出风格 */
public record OcrOptions(String languageHint, boolean markdownTables) {

    public static OcrOptions defaults() {
        return new OcrOptions("zh", true);
    }
}
