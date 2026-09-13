package com.gymmind.iam.api;

import com.gymmind.iam.application.UserAdministrationService;
import com.gymmind.iam.application.command.CreateUserCommand;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserStatus;
import com.gymmind.shared.api.PageResponse;
import com.gymmind.shared.api.TraceIdFilter;
import com.gymmind.shared.error.GlobalExceptionHandler;
import com.gymmind.shared.security.CurrentActorProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static com.gymmind.support.SecurityTestActors.gymAdmin;
import static com.gymmind.support.SecurityTestActors.platformAdmin;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

    private UserAdministrationService service;
    private CurrentActorProvider actors;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(UserAdministrationService.class);
        actors = mock(CurrentActorProvider.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(service, actors), new RoleController(service, actors))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .addFilters(new TraceIdFilter())
                .build();
    }

    @Test
    void gymAdminCanCreateListChangeStatusAndRoles() throws Exception {
        when(actors.requireCurrent()).thenReturn(gymAdmin());
        when(service.create(any(), any())).thenReturn(new UserAdministrationService.UserView(
                20L, 10L, "member@example.com", "Member", UserStatus.ACTIVE, Set.of(RoleCode.MEMBER)));
        when(service.list(any(), any())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));
        when(service.listRoles(any())).thenReturn(List.of(new UserAdministrationService.RoleView(3L, RoleCode.MEMBER, "Member")));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"member@example.com\",\"password\":\"password123\",\"displayName\":\"Member\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(20));
        mockMvc.perform(get("/api/v1/users")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/users/20/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/users/20/roles")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"COACH\"]}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/roles")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("MEMBER"));

        verify(service).create(any(), any(CreateUserCommand.class));
        verify(service).list(any(), any());
        verify(service).changeStatus(any(), org.mockito.ArgumentMatchers.eq(20L), org.mockito.ArgumentMatchers.eq(UserStatus.DISABLED));
        verify(service).replaceRoles(any(), org.mockito.ArgumentMatchers.eq(20L), org.mockito.ArgumentMatchers.eq(Set.of(RoleCode.COACH)));
    }

    @Test
    void platformAdminCannotReadTenantUsers() throws Exception {
        when(actors.requireCurrent()).thenReturn(platformAdmin());
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        verify(service, never()).list(any(), any());
    }
}
