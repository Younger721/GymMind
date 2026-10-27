package com.gymmind.knowledge.application;

import com.gymmind.knowledge.config.KnowledgeOnlyOfficeProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** 对 OnlyOffice 编辑器配置做 JWT 签名（与 Document Server JWT_SECRET 一致） */
@Component
public class OnlyOfficeJwtSigner {

    private final KnowledgeOnlyOfficeProperties properties;

    public OnlyOfficeJwtSigner(KnowledgeOnlyOfficeProperties properties) {
        this.properties = properties;
    }

    public String signConfig(Map<String, Object> config) {
        // 与 Node jwt.sign(config) 一致：payload 为 config 对象本身（标准 JWT claims）
        SecretKey key = new SecretKeySpec(
                properties.getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return Jwts.builder().claims(config).signWith(key).compact();
    }

    public String verifyCallbackToken(String token, long expectedDocumentId) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token).getPayload();
            String key = claims.get("key", String.class);
            if (key == null || !key.startsWith(expectedDocumentId + "-")) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            return key;
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private SecretKey signingKey() {
        return new SecretKeySpec(
                properties.getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
