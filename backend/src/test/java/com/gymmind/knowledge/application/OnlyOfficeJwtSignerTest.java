package com.gymmind.knowledge.application;

import com.gymmind.knowledge.config.KnowledgeOnlyOfficeProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OnlyOfficeJwtSignerTest {

    @Test
    void signsConfigWithDocumentClaimAtJwtRoot() {
        var props = new KnowledgeOnlyOfficeProperties();
        props.setJwtSecret("GymMindOnlyOfficeDevSecretChangeMe32");
        var signer = new OnlyOfficeJwtSigner(props);

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("url", "http://localhost:8080/doc.docx");
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("document", document);
        config.put("documentType", "word");

        String token = signer.signConfig(config);
        var key = Keys.hmacShaKeyFor("GymMindOnlyOfficeDevSecretChangeMe32".getBytes(StandardCharsets.UTF_8));
        var payload = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        assertThat(payload.get("documentType")).isEqualTo("word");
        assertThat(payload.get("document")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> parsedDoc = (Map<String, Object>) payload.get("document");
        assertThat(parsedDoc.get("url")).isEqualTo("http://localhost:8080/doc.docx");
    }

    @Test
    void verifiesValidCallbackTokenAndRejectsInvalidToken() {
        var props = new KnowledgeOnlyOfficeProperties();
        props.setJwtSecret("GymMindOnlyOfficeDevSecretChangeMe32");
        var signer = new OnlyOfficeJwtSigner(props);
        String token = signer.signConfig(Map.of("status", 2, "key", "42-1710000000000"));

        assertThatCode(() -> signer.verifyCallbackToken(token, 42L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> signer.verifyCallbackToken(token, 43L))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
        assertThatThrownBy(() -> signer.verifyCallbackToken("invalid-token", 42L))
                .isInstanceOf(com.gymmind.shared.error.BusinessException.class);
    }
}
