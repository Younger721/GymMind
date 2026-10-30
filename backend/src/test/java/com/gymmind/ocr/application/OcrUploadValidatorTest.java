package com.gymmind.ocr.application;

import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** 上传校验单测：类型白名单、魔数一致性、大小上限 */
class OcrUploadValidatorTest {

    private static final long MAX = 10 * 1024 * 1024L;

    @Test
    void 空内容校验失败() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> OcrUploadValidator.validate("image/png", new byte[0], MAX));
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.errorCode());
    }

    @Test
    void 类型不在白名单校验失败() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> OcrUploadValidator.validate("image/gif", new byte[]{1, 2, 3}, MAX));
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.errorCode());
    }

    @Test
    void PNG魔数一致校验通过() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A};
        assertDoesNotThrow(() -> OcrUploadValidator.validate("image/png", png, MAX));
    }

    @Test
    void 伪造contentType魔数不一致校验失败() {
        // 声明 PNG 实际是文本：必须拒绝
        byte[] fake = "hello".getBytes();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> OcrUploadValidator.validate("image/png", fake, MAX));
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.errorCode());
    }

    @Test
    void JPEG魔数一致校验通过() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        assertDoesNotThrow(() -> OcrUploadValidator.validate("image/jpeg", jpeg, MAX));
    }

    @Test
    void WebP魔数一致校验通过() {
        byte[] webp = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
        assertDoesNotThrow(() -> OcrUploadValidator.validate("image/webp", webp, MAX));
    }

    @Test
    void 超过大小上限校验失败() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G'};
        BusinessException ex = assertThrows(BusinessException.class,
                () -> OcrUploadValidator.validate("image/png", png, 2));
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.errorCode());
    }
}
