package com.gymmind.shared.security;

import com.gymmind.iam.application.SessionService;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.jwt.JwtClaims;
import com.gymmind.shared.security.jwt.JwtService;
import com.gymmind.shared.security.jwt.TokenType;
import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    private static final Instant NOW = Instant.parse("2026-09-12T09:00:00Z");
    private JwtService jwt = mock(JwtService.class);
    private SessionService sessions = mock(SessionService.class);
    private UserAccountRepository users = mock(UserAccountRepository.class);
    private UserRoleRepository userRoles = mock(UserRoleRepository.class);
    private TenantRepository tenants = mock(TenantRepository.class);
    private PrincipalLoader loader;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        loader = new PrincipalLoader(sessions, users, userRoles, tenants);
        filter = new JwtAuthenticationFilter(jwt, loader, new RestAuthenticationEntryPoint(new com.fasterxml.jackson.databind.ObjectMapper()));
        UserAccount user = UserAccount.tenantUser(7L, "owner@example.com", "hash", "Owner", RoleCode.MEMBER);
        ReflectionTestUtils.setField(user, "id", 42L);
        Tenant tenant = Tenant.create("gym", "Gym");
        ReflectionTestUtils.setField(tenant, "id", 7L);
        when(users.findById(42L)).thenReturn(Optional.of(user));
        when(tenants.findById(7L)).thenReturn(Optional.of(tenant));
        when(userRoles.findRoleCodesByUserId(42L)).thenReturn(Set.of(RoleCode.GYM_ADMIN));
        when(userRoles.findPermissionCodesByUserId(42L)).thenReturn(Set.of("tenant:settings:write"));
        when(jwt.verify("valid", TokenType.ACCESS)).thenReturn(claims(7L));
    }

    @Test
    void loadsRepositoryAuthoritiesInsteadOfTrustingJwtClaims() throws Exception {
        MockHttpServletResponse response = request("Bearer valid", () -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(((GymMindPrincipal) authentication.getPrincipal()).actor().roles())
                    .containsExactly(RoleCode.GYM_ADMIN);
            assertThat(((GymMindPrincipal) authentication.getPrincipal()).actor().permissions())
                    .containsExactly("tenant:settings:write");
            assertThat(authentication.getAuthorities()).extracting("authority")
                    .containsExactlyInAnyOrder("ROLE_GYM_ADMIN", "tenant:settings:write");
        });
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(sessions).isAccessRevoked(7L, 42L, "access-jti");
    }

    @Test
    void missingBearerLeavesRequestAnonymous() throws Exception {
        request(null, () -> assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull());
        verifyNoInteractions(jwt, sessions);
    }

    @Test
    void malformedBearerAndWrongTokenTypeReturn401() throws Exception {
        assertUnauthorized("Basic abc");
        assertUnauthorized("Bearer ");
        when(jwt.verify("refresh", TokenType.ACCESS)).thenReturn(new JwtClaims(
                "GymMind", 42L, 7L, Set.of(RoleCode.MEMBER), Set.of("stale"), 0L,
                "access-jti", TokenType.REFRESH, NOW, NOW.plusSeconds(300)));
        assertUnauthorized("Bearer refresh");
        verifyNoInteractions(sessions);
    }

    @Test
    void revokedAndDependencyFailuresReturn401() throws Exception {
        when(sessions.isAccessRevoked(7L, 42L, "access-jti")).thenReturn(true);
        assertUnauthorized("Bearer valid");
        doThrow(new IllegalStateException("redis unavailable"))
                .when(sessions).isAccessRevoked(7L, 42L, "access-jti");
        assertUnauthorized("Bearer valid");
        doReturn(false).when(sessions).isAccessRevoked(7L, 42L, "access-jti");
        when(users.findById(42L)).thenThrow(new IllegalStateException("database unavailable"));
        assertUnauthorized("Bearer valid");
    }

    @Test
    void missingDisabledOrMismatchedUserAndTenantReturn401() throws Exception {
        when(users.findById(42L)).thenReturn(Optional.empty());
        assertUnauthorized("Bearer valid");
        UserAccount user = UserAccount.tenantUser(7L, "owner@example.com", "hash", "Owner");
        ReflectionTestUtils.setField(user, "id", 42L);
        when(users.findById(42L)).thenReturn(Optional.of(user));
        user.disable();
        assertUnauthorized("Bearer valid");
        user.activate();
        assertUnauthorized("Bearer valid"); // disabled user incremented tokenVersion
        UserAccount wrongScope = UserAccount.tenantUser(8L, "owner@example.com", "hash", "Owner");
        ReflectionTestUtils.setField(wrongScope, "id", 42L);
        when(users.findById(42L)).thenReturn(Optional.of(wrongScope));
        assertUnauthorized("Bearer valid");
        UserAccount active = UserAccount.tenantUser(7L, "owner@example.com", "hash", "Owner");
        ReflectionTestUtils.setField(active, "id", 42L);
        when(users.findById(42L)).thenReturn(Optional.of(active));
        when(tenants.findById(7L)).thenReturn(Optional.empty());
        assertUnauthorized("Bearer valid");
        Tenant disabled = Tenant.create("gym", "Gym");
        disabled.disable();
        when(tenants.findById(7L)).thenReturn(Optional.of(disabled));
        assertUnauthorized("Bearer valid");
        Tenant wrongId = Tenant.create("other", "Other");
        ReflectionTestUtils.setField(wrongId, "id", 8L);
        when(tenants.findById(7L)).thenReturn(Optional.of(wrongId));
        assertUnauthorized("Bearer valid");
    }

    @Test
    void invalidAuthorizationStateAndRepositoryFailureReturn401() throws Exception {
        when(userRoles.findRoleCodesByUserId(42L)).thenReturn(Set.of());
        assertUnauthorized("Bearer valid");
        when(userRoles.findRoleCodesByUserId(42L)).thenReturn(Set.of(RoleCode.PLATFORM_ADMIN));
        assertUnauthorized("Bearer valid");
        when(userRoles.findRoleCodesByUserId(42L)).thenThrow(new IllegalStateException("roles unavailable"));
        assertUnauthorized("Bearer valid");
    }

    @Test
    void platformAdminHasNoTenantLookupAndRejectsNonPlatformRoles() {
        UserAccount platform = UserAccount.platformAdmin("root@example.com", "hash", "Root");
        ReflectionTestUtils.setField(platform, "id", 42L);
        when(users.findById(42L)).thenReturn(Optional.of(platform));
        when(userRoles.findRoleCodesByUserId(42L)).thenReturn(Set.of(RoleCode.PLATFORM_ADMIN));
        CurrentActor actor = loader.load(claims(null));
        assertThat(actor.isPlatformAdmin()).isTrue();
        verifyNoInteractions(tenants);
        when(userRoles.findRoleCodesByUserId(42L)).thenReturn(Set.of(RoleCode.MEMBER));
        assertThatThrownBy(() -> loader.load(claims(null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        failure -> assertThat(failure.errorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
    }

    private JwtClaims claims(Long tenantId) {
        return new JwtClaims("GymMind", 42L, tenantId,
                Set.of(tenantId == null ? RoleCode.PLATFORM_ADMIN : RoleCode.MEMBER), Set.of("stale"),
                0L, "access-jti", TokenType.ACCESS, NOW, NOW.plusSeconds(300));
    }

    private void assertUnauthorized(String authorization) throws Exception {
        MockHttpServletResponse response = request(authorization, () -> {
            throw new AssertionError("invalid bearer reached downstream");
        });
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"code\":\"UNAUTHENTICATED\"")
                .doesNotContain("unavailable");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockHttpServletResponse request(String authorization, Runnable downstream) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test-secured");
        if (authorization != null) request.addHeader("Authorization", authorization);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> downstream.run());
        return response;
    }
}
