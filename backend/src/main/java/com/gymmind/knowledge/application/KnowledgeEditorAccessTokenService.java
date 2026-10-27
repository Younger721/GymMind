package com.gymmind.knowledge.application;

import com.gymmind.knowledge.config.KnowledgeOnlyOfficeProperties;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;

/** OnlyOffice 下载/回调用的短期 HMAC 令牌（无法携带 Bearer） */
@Service
public class KnowledgeEditorAccessTokenService {

    private final KnowledgeOnlyOfficeProperties properties;
    private final Clock clock;

    public KnowledgeEditorAccessTokenService(KnowledgeOnlyOfficeProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public String issueDownloadToken(long documentId, long userId, long tenantId) {
        return sign(documentId, userId, tenantId, "dl");
    }

    public String issueCallbackToken(long documentId, long userId, long tenantId) {
        return sign(documentId, userId, tenantId, "cb");
    }

    public long verifyDownloadToken(long documentId, long userId, String token) {
        return verify(documentId, userId, "dl", token);
    }

    public long verifyCallbackToken(long documentId, long userId, String token) {
        return verify(documentId, userId, "cb", token);
    }

    private String sign(long documentId, long userId, long tenantId, String purpose) {
        Instant expires = clock.instant().plus(properties.getDownloadTokenTtl());
        String payload = documentId + "|" + userId + "|" + tenantId + "|" + purpose + "|" + expires.getEpochSecond();
        return payload + "|" + hmac(payload);
    }

    private long verify(long documentId, long userId, String purpose, String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        String[] parts = token.split("\\|");
        if (parts.length != 6) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        long tokenDocumentId;
        long tokenUserId;
        long tokenTenantId;
        long expiresEpoch;
        try {
            tokenDocumentId = Long.parseLong(parts[0]);
            tokenUserId = Long.parseLong(parts[1]);
            tokenTenantId = Long.parseLong(parts[2]);
            expiresEpoch = Long.parseLong(parts[4]);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        if (tokenDocumentId != documentId || tokenUserId != userId || tokenTenantId <= 0 || !purpose.equals(parts[3])) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        if (clock.instant().getEpochSecond() > expiresEpoch) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        String payload = parts[0] + "|" + parts[1] + "|" + parts[2] + "|" + parts[3] + "|" + parts[4];
        if (!hmac(payload).equals(parts[5])) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        return tokenTenantId;
    }

    private String hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("HMAC unavailable", ex);
        }
    }
}
