package com.gymmind.audit.api;

import com.gymmind.ai.application.AiUsageRecord;
import com.gymmind.audit.application.*;
import com.gymmind.audit.domain.OperationAudit;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuditQueryControllerTest {
    private final AuditQueryService audits = mock(AuditQueryService.class);
    private final AiUsageQueryService usage = mock(AiUsageQueryService.class);
    private final CurrentActor actor = new CurrentActor(2L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of("audit:read"), 0, "t");
    private final CurrentActorProvider actors = new CurrentActorProvider() {
        public Optional<CurrentActor> current() { return Optional.of(actor); }
        public CurrentActor requireCurrent() { return actor; }
    };
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuditQueryController(audits, usage, actors)).build();

    @Test
    void exposesTenantScopedAiUsage() throws Exception {
        when(usage.list(actor)).thenReturn(List.of(new AiUsageRecord(7L, 2L, "qwen3.7-plus", 1, 2, 3, "r", "OK", null)));
        mvc.perform(get("/api/v1/audits/ai-usage"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].tenantId").value(7));
        verify(usage).list(actor);
    }
}
