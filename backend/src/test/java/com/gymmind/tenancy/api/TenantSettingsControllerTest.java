package com.gymmind.tenancy.api;

import com.gymmind.shared.api.TraceIdFilter;
import com.gymmind.shared.error.GlobalExceptionHandler;
import com.gymmind.shared.security.CurrentActorProvider;
import com.gymmind.tenancy.application.TenantSettingsService;
import com.gymmind.tenancy.api.response.TenantSettingsView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.gymmind.support.SecurityTestActors.gymAdmin;
import static com.gymmind.support.SecurityTestActors.platformAdmin;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TenantSettingsControllerTest {

    private TenantSettingsService service;
    private CurrentActorProvider actors;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(TenantSettingsService.class);
        actors = mock(CurrentActorProvider.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new TenantSettingsController(service, actors))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new TraceIdFilter())
                .build();
    }

    @Test
    void currentGymAdminReadsAndUpdatesSettingsWithoutClientTenantId() throws Exception {
        when(actors.requireCurrent()).thenReturn(gymAdmin());
        when(service.getCurrent(any())).thenReturn(new TenantSettingsView(10L, true, true, "Asia/Shanghai"));
        when(service.updateCurrent(any(), any())).thenReturn(new TenantSettingsView(10L, false, true, "Asia/Shanghai"));

        mockMvc.perform(get("/api/v1/tenant/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").value(10))
                .andExpect(jsonPath("$.data.reservationEnabled").value(true));

        mockMvc.perform(put("/api/v1/tenant/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reservationEnabled":false,"checkInEnabled":true,
                                 "timezone":"Asia/Shanghai","tenantId":999}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").value(10))
                .andExpect(jsonPath("$.data.reservationEnabled").value(false));

        verify(service).getCurrent(gymAdmin());
        verify(service).updateCurrent(gymAdmin(),
                new com.gymmind.tenancy.application.command.UpdateTenantSettingsCommand(
                        false, true, "Asia/Shanghai"));
    }

    @Test
    void platformAdminCannotReadTenantSettings() throws Exception {
        when(actors.requireCurrent()).thenReturn(platformAdmin());

        mockMvc.perform(get("/api/v1/tenant/settings"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(service, never()).getCurrent(any());
    }

    @Test
    void invalidSettingsBodyReturnsValidationError() throws Exception {
        mockMvc.perform(put("/api/v1/tenant/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
