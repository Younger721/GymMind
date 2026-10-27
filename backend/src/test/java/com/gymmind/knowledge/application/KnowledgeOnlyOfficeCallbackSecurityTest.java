package com.gymmind.knowledge.application;

import com.gymmind.knowledge.config.KnowledgeOnlyOfficeProperties;
import com.gymmind.shared.error.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClient;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Map;
import java.util.List;

import static org.mockito.Mockito.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeOnlyOfficeCallbackSecurityTest {

    @Test
    void acceptsOnlyConfiguredHttpCallbackHosts() {
        var properties = properties();

        URI uri = KnowledgeOnlyOfficeService.validateCallbackUrl("http://onlyoffice/cache/file.docx", properties);

        assertThat(uri.getHost()).isEqualTo("onlyoffice");
    }

    @Test
    void rejectsCallbackWithUnconfiguredPortOrScheme() {
        assertThatThrownBy(() -> KnowledgeOnlyOfficeService.validateCallbackUrl(
                "http://onlyoffice:8081/cache/file.docx", properties()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> KnowledgeOnlyOfficeService.validateCallbackUrl(
                "https://onlyoffice:8080/cache/file.docx", properties()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsCallbackJwtForDifferentDocumentKey() {
        var properties = properties();
        var signer = new OnlyOfficeJwtSigner(properties);
        var service = new KnowledgeOnlyOfficeService(properties, mock(KnowledgeDocumentUseCase.class),
                new KnowledgeEditorAccessTokenService(properties, Clock.systemUTC()), signer,
                mock(KnowledgeEditorActorResolver.class), RestClient.builder());
        String signedKey = "42-1710000000000";
        var body = new ObjectMapper().valueToTree(Map.of(
                "status", 2,
                "key", "43-1710000000000",
                "token", signer.signConfig(Map.of("key", signedKey))));
        String session = new KnowledgeEditorAccessTokenService(properties, Clock.systemUTC())
                .issueCallbackToken(42L, 7L, 11L);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.handleCallback(42L, 7L, session, body))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsPrivateNetworkCallbackUrlNotOnAllowList() {
        assertThatThrownBy(() -> KnowledgeOnlyOfficeService.validateCallbackUrl(
                "http://169.254.169.254/latest/meta-data/", properties()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsNonHttpAndCredentialBearingCallbackUrls() {
        assertThatThrownBy(() -> KnowledgeOnlyOfficeService.validateCallbackUrl("file:///etc/passwd", properties()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> KnowledgeOnlyOfficeService.validateCallbackUrl(
                "http://user:pass@onlyoffice/file.docx", properties()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void readsOnlyWithinConfiguredMaximumBytes() throws Exception {
        byte[] content = "docx".getBytes(StandardCharsets.UTF_8);
        assertThat(KnowledgeOnlyOfficeService.readBounded(new ByteArrayInputStream(content), 4))
                .containsExactly(content);

        assertThatThrownBy(() -> KnowledgeOnlyOfficeService.readBounded(
                new ByteArrayInputStream(content), 3))
                .isInstanceOf(BusinessException.class);
    }

    private static KnowledgeOnlyOfficeProperties properties() {
        var properties = new KnowledgeOnlyOfficeProperties();
        properties.setCallbackAllowedHosts(List.of("onlyoffice"));
        properties.setCallbackAllowedPorts(List.of(80));
        properties.setCallbackMaxBytes(16);
        return properties;
    }
}
