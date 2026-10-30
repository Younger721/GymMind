package com.gymmind.ocr.application;

import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;

import java.util.Locale;
import java.util.Set;

/** OCR 上传校验：内容类型白名单 + 魔数一致性 + 大小上限（仿知识库 PDF 魔数校验写法） */
public final class OcrUploadValidator {

    /** 支持的图片类型 → 文件魔数 */
    private static final Set<String> SUPPORTED = Set.of("image/jpeg", "image/png", "image/webp", "image/bmp");

    private OcrUploadValidator() {
    }

    /** 校验失败抛 {@link ErrorCode#VALIDATION_FAILED} */
    public static void validate(String contentType, byte[] content, long maxBytes) {
        if (contentType == null || content == null || content.length == 0) {
            throw validationFailed("图片内容为空");
        }
        if (content.length > maxBytes) {
            throw validationFailed("图片大小超过上限 " + (maxBytes / 1024 / 1024) + "MB");
        }
        String type = contentType.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED.contains(type)) {
            throw validationFailed("不支持的图片类型: " + type);
        }
        if (!magicMatches(type, content)) {
            throw validationFailed("图片内容与声明类型不一致");
        }
    }

    /** 魔数与声明类型比对，防止伪造 contentType */
    private static boolean magicMatches(String type, byte[] b) {
        return switch (type) {
            // JPEG: FF D8 FF
            case "image/jpeg" -> b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
            // PNG: 89 50 4E 47
            case "image/png" -> b.length >= 4 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
            // WebP: RIFF .... WEBP
            case "image/webp" -> b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                    && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
            // BMP: 42 4D
            case "image/bmp" -> b.length >= 2 && b[0] == 'B' && b[1] == 'M';
            default -> false;
        };
    }

    private static BusinessException validationFailed(String message) {
        return new BusinessException(ErrorCode.VALIDATION_FAILED, message);
    }
}
