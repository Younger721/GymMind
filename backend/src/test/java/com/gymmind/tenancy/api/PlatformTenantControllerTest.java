package com.gymmind.tenancy.api;

import com.gymmind.shared.api.TraceIdFilter;
import com.gymmind.shared.error.GlobalExceptionHandler;
import com.gymmind.shared.security.CurrentActorProvider;
import com.gymmind.shared.security.TenantAccessGuard;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.application.PlatformTenantService;
import com.gymmind.tenancy.api.response.TenantView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import java.util.Optional;

import static com.gymmind.support.SecurityTestActors.gymAdmin;
import static com.gymmind.support.SecurityTestActors.platformAdmin;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlatformTenantControllerTest {

    private PlatformTenantService service;
    private CurrentActorProvider actors;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(PlatformTenantService.class);
        actors = mock(CurrentActorProvider.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PlatformTenantController(service, actors))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .addFilters(new TraceIdFilter())
                .build();
    }

    @Test
    void platformAdminCanCreateListAndChangeTenantStatus() throws Exception {
        when(actors.requireCurrent()).thenReturn(platformAdmin());
        when(service.create(any(), any())).thenReturn(new TenantView(7L, "gym-a", "Gym A", "ACTIVE"));
        when(service.list(any(), any())).thenReturn(new com.gymmind.shared.api.PageResponse<>(
                java.util.List.of(new TenantView(7L, "gym-a", "Gym A", "ACTIVE")), 0, 20, 1, 1));
        when(service.disable(7L, platformAdmin())).thenReturn(new TenantView(7L, "gym-a", "Gym A", "DISABLED"));

        mockMvc.perform(post("/api/v1/platform/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantCode":"gym-a","tenantName":"Gym A",
                                 "adminEmail":"admin@example.com","adminPassword":"password123",
                                 "adminDisplayName":"Admin"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.code").value("gym-a"));

        mockMvc.perform(get("/api/v1/platform/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(7));

        mockMvc.perform(patch("/api/v1/platform/tenants/7/disable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        verify(service).create(any(), eq(platformAdmin()));
        verify(service).list(any(), any());
        verify(service).disable(7L, platformAdmin());
    }

    @Test
    void tenantActorCannotUsePlatformTenantEndpoints() throws Exception {
        when(actors.requireCurrent()).thenReturn(gymAdmin());

        mockMvc.perform(get("/api/v1/platform/tenants"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(service, never()).list(any(), any());
    }

    @Test
    void invalidCreateBodyReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/platform/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
