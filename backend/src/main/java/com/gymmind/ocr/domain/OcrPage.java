package com.gymmind.ocr.domain;

/**
 * 单页识别结果：{@code confidence} 可空（供应商不保证返回）。
 */
public record OcrPage(int pageNumber, String text, Double confidence) {
}
