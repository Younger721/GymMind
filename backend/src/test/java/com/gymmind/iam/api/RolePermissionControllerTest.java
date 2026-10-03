package com.gymmind.iam.api;

import com.gymmind.iam.application.UserAdministrationService;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.api.TraceIdFilter;
import com.gymmind.shared.error.GlobalExceptionHandler;
import com.gymmind.shared.security.CurrentActorProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static com.gymmind.support.SecurityTestActors.gymAdmin;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RolePermissionControllerTest {
    private UserAdministrationService service;
    private CurrentActorProvider actors;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(UserAdministrationService.class);
        actors = mock(CurrentActorProvider.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RoleController(service, actors))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new TraceIdFilter())
                .build();
    }

    @Test
    void gymAdminCanReadAndUpdateScopedRoleMenus() throws Exception {
        when(actors.requireCurrent()).thenReturn(gymAdmin());
        when(service.listPermissionCatalog(any())).thenReturn(List.of(
                new UserAdministrationService.PermissionView("menu:training", "Training", Set.of(RoleCode.COACH))));

        mockMvc.perform(get("/api/v1/roles/permissions"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/roles/COACH/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissions\":[\"menu:training\"]}"))
                .andExpect(status().isOk());

        verify(service).listPermissionCatalog(gymAdmin());
        verify(service).replaceRolePermissions(eq(gymAdmin()), eq(RoleCode.COACH), eq(Set.of("menu:training")));
    }
}
