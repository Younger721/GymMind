package com.gymmind.iam.api;

import com.gymmind.iam.application.AuthApplicationService;
import com.gymmind.iam.application.command.LoginCommand;
import com.gymmind.iam.application.command.LogoutCommand;
import com.gymmind.iam.application.command.RefreshCommand;
import com.gymmind.iam.application.command.RegisterTenantCommand;
import com.gymmind.iam.application.result.AuthResult;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.api.TraceIdFilter;
import com.gymmind.shared.error.GlobalExceptionHandler;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.CurrentActorProvider;
import com.gymmind.shared.security.jwt.TokenPair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private static final CurrentActor ACTOR = new CurrentActor(
            42L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of("tenant:settings:write"), 0L, "access-jti");
    private static final AuthResult RESULT = new AuthResult(
            new AuthResult.AuthenticatedUser(
                    42L, 7L, "owner@example.com", "Owner",
                    Set.of(RoleCode.GYM_ADMIN), Set.of("tenant:settings:write")),
            new TokenPair(
                    "access-token", Instant.parse("2026-09-13T02:15:00Z"),
                    "refresh-token", Instant.parse("2026-09-20T02:00:00Z")));

    private AuthApplicationService service;
    private CurrentActorProvider actors;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(AuthApplicationService.class);
        actors = mock(CurrentActorProvider.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(service, actors))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new TraceIdFilter())
                .build();
    }

    @Test
    void registerMapsValidatedRequestAndReturnsCreatedAuthEnvelope() throws Exception {
        when(service.registerTenant(any())).thenReturn(RESULT);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantCode":"gym-a","tenantName":"Gym A",
                                 "email":" Owner@Example.COM ","password":"password123","displayName":"Owner"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(42))
                .andExpect(jsonPath("$.data.tenantId").value(7))
                .andExpect(jsonPath("$.data.email").value("owner@example.com"))
                .andExpect(jsonPath("$.data.roles[0]").value("GYM_ADMIN"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));

        ArgumentCaptor<RegisterTenantCommand> command = ArgumentCaptor.forClass(RegisterTenantCommand.class);
        verify(service).registerTenant(command.capture());
        assertThat(command.getValue()).isEqualTo(new RegisterTenantCommand(
                "gym-a", "Gym A", "Owner@Example.COM", "password123", "Owner"));
    }

    @Test
    void loginAndRefreshMapRequestsAndReturnTokens() throws Exception {
        when(service.login(any())).thenReturn(RESULT);
        when(service.refresh(any())).thenReturn(RESULT);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\" owner@example.com \",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"raw-refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));

        verify(service).login(new LoginCommand("owner@example.com", "password123"));
        verify(service).refresh(new RefreshCommand("raw-refresh-token"));
    }

    @Test
    void logoutUsesAuthenticatedActorBearerAccessTokenAndBodyRefreshToken() throws Exception {
        when(actors.requireCurrent()).thenReturn(ACTOR);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer raw-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"raw-refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(service).logout(ACTOR, new LogoutCommand("raw-access-token", "raw-refresh-token"));
    }

    @Test
    void invalidBodiesAndMalformedLogoutHeaderUseStableErrorsWithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Basic raw-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"raw-refresh-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        verify(service, never()).registerTenant(any());
        verify(service, never()).logout(any(), any());
    }
}
