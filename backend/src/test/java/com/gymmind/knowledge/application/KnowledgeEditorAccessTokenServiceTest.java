package com.gymmind.knowledge.application;

import com.gymmind.knowledge.config.KnowledgeOnlyOfficeProperties;
import com.gymmind.shared.error.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeEditorAccessTokenServiceTest {

    @Test
    void verifiesMatchingDownloadToken() {
        var clock = Clock.fixed(Instant.parse("2026-10-13T00:00:00Z"), ZoneOffset.UTC);
        var service = new KnowledgeEditorAccessTokenService(properties(), clock);
        String token = service.issueDownloadToken(9L, 4L, 7L);
        assertThatCode(() -> service.verifyDownloadToken(9L, 4L, token)).doesNotThrowAnyException();
    }

    @Test
    void rejectsExpiredToken() {
        var clock = Clock.fixed(Instant.parse("2026-10-13T00:00:00Z"), ZoneOffset.UTC);
        var service = new KnowledgeEditorAccessTokenService(properties(), clock);
        String token = service.issueDownloadToken(9L, 4L, 7L);
        var later = Clock.fixed(Instant.parse("2026-10-13T01:00:00Z"), ZoneOffset.UTC);
        var expiredService = new KnowledgeEditorAccessTokenService(properties(), later);
        assertThatThrownBy(() -> expiredService.verifyDownloadToken(9L, 4L, token))
                .isInstanceOf(BusinessException.class);
    }

    private static KnowledgeOnlyOfficeProperties properties() {
        var props = new KnowledgeOnlyOfficeProperties();
        props.setJwtSecret("test-onlyoffice-secret-32bytes!!");
        props.setDownloadTokenTtl(Duration.ofMinutes(15));
        return props;
    }
}
