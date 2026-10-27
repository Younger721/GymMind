package com.gymmind.knowledge.application;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.knowledge.config.KnowledgeOnlyOfficeProperties;
import com.gymmind.knowledge.domain.model.DocumentStatus;
import com.gymmind.knowledge.domain.model.DocumentVisibility;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.TenantContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeOnlyOfficeServiceTest {

    @Test
    void platformAdminGetsEditableEditorConfigWithSaveCallback() {
        var properties = new KnowledgeOnlyOfficeProperties();
        properties.setEnabled(true);
        var documents = mock(KnowledgeDocumentUseCase.class);
        when(documents.find(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(42L)))
                .thenReturn(new KnowledgeDocumentView(42L, 7L, null, "guide.docx", "tenant/7/guide.docx",
                        DocumentVisibility.TENANT, DocumentStatus.READY));
        var tokens = new KnowledgeEditorAccessTokenService(properties, java.time.Clock.systemUTC());
        var service = new KnowledgeOnlyOfficeService(properties, documents, tokens, new OnlyOfficeJwtSigner(properties),
                mock(KnowledgeEditorActorResolver.class), RestClient.builder());
        var platform = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN), Set.of(), 0L, "platform");

        TenantContextHolder.set(7L);
        KnowledgeOnlyOfficeEditorView editor;
        try {
            editor = service.buildEditorConfig(platform, 42L);
        } finally {
            TenantContextHolder.clear();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> document = (Map<String, Object>) editor.config().get("document");
        @SuppressWarnings("unchecked")
        Map<String, Object> permissions = (Map<String, Object>) document.get("permissions");
        @SuppressWarnings("unchecked")
        Map<String, Object> editorConfig = (Map<String, Object>) editor.config().get("editorConfig");
        assertThat(permissions.get("edit")).isEqualTo(true);
        assertThat(editorConfig.get("mode")).isEqualTo("edit");
        assertThat(editorConfig).containsKey("callbackUrl");
    }
}
