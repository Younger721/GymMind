package com.gymmind.knowledge.api;

import com.gymmind.knowledge.application.KnowledgeDocumentUseCase;
import com.gymmind.knowledge.application.KnowledgeDocumentView;
import com.gymmind.knowledge.domain.model.DocumentStatus;
import com.gymmind.knowledge.domain.model.DocumentVisibility;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.CurrentActorProvider;
import com.gymmind.iam.domain.model.RoleCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class KnowledgeDocumentControllerTest {
    private final KnowledgeDocumentUseCase useCase = mock(KnowledgeDocumentUseCase.class);
    private final CurrentActor actor = new CurrentActor(4L, 9L, Set.of(RoleCode.GYM_ADMIN), Set.of("knowledge:write"), 0, "token");
    private final CurrentActorProvider actors = new CurrentActorProvider() {
        public Optional<CurrentActor> current() { return Optional.of(actor); }
        public CurrentActor requireCurrent() { return actor; }
    };
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new KnowledgeDocumentController(useCase, actors)).build();

    @Test
    void uploadsMultipartDocumentAndReturnsAccepted() throws Exception {
        when(useCase.upload(eq(actor), any())).thenReturn(new KnowledgeDocumentView(1L, 9L, null, "guide.txt", "tenant/9/knowledge/1/guide.txt", DocumentVisibility.TENANT, DocumentStatus.UPLOADING));
        MockMultipartFile file = new MockMultipartFile("file", "guide.txt", MediaType.TEXT_PLAIN_VALUE, "guide".getBytes());
        mvc.perform(multipart("/api/v1/knowledge/documents").file(file).param("visibility", "TENANT"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true));
        verify(useCase).upload(eq(actor), any());
    }

    @Test
    void reindexesAndDeletesThroughTenantScopedUseCase() throws Exception {
        mvc.perform(post("/api/v1/knowledge/documents/3/reindex")).andExpect(status().isAccepted());
        mvc.perform(delete("/api/v1/knowledge/documents/3")).andExpect(status().isNoContent());
        verify(useCase).reindex(actor, 3L);
        verify(useCase).delete(actor, 3L);
    }
}
