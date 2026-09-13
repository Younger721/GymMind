package com.gymmind.iam.api;

import com.gymmind.iam.application.UserInvitationService;
import com.gymmind.iam.application.result.AuthResult;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.api.TraceIdFilter;
import com.gymmind.shared.error.GlobalExceptionHandler;
import com.gymmind.shared.security.CurrentActorProvider;
import com.gymmind.shared.security.jwt.TokenPair;
import com.gymmind.iam.domain.model.RoleCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Set;

import static com.gymmind.support.SecurityTestActors.gymAdmin;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserInvitationControllerTest {

    private UserInvitationService service;
    private CurrentActorProvider actors;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(UserInvitationService.class);
        actors = mock(CurrentActorProvider.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new UserInvitationController(service, actors), new AuthInvitationController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new TraceIdFilter())
                .build();
    }

    @Test
    void gymAdminCanIssueInvitationAndAcceptEndpointReturnsAuth() throws Exception {
        when(actors.requireCurrent()).thenReturn(gymAdmin());
        when(service.invite(any(), any())).thenReturn(new UserInvitationService.InvitationIssued(
                "raw-token", 10L, "member@example.com", RoleCode.MEMBER,
                Instant.parse("2026-09-14T00:00:00Z")));
        when(service.accept(any())).thenReturn(new AuthResult(
                new AuthResult.AuthenticatedUser(40L, 10L, "member@example.com", "Member",
                        Set.of(RoleCode.MEMBER), Set.of()),
                new TokenPair("access-token", Instant.parse("2026-09-13T01:00:00Z"),
                        "refresh-token", Instant.parse("2026-09-20T01:00:00Z"))));

        mockMvc.perform(post("/api/v1/users/invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"member@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rawToken").value("raw-token"));

        mockMvc.perform(post("/api/v1/auth/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"raw-token\",\"password\":\"ValidPassword123!\",\"displayName\":\"Member\"}"))
                .andExpect(status().isOk());
    }
}
