package com.gymmind.iam.application;

import com.gymmind.iam.application.command.AcceptInvitationCommand;
import com.gymmind.iam.application.command.InviteUserCommand;
import com.gymmind.iam.domain.model.Role;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.model.UserInvitation;
import com.gymmind.iam.domain.repository.RoleRepository;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.iam.domain.repository.UserInvitationRepository;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.jwt.JwtClaims;
import com.gymmind.shared.security.jwt.JwtService;
import com.gymmind.shared.security.jwt.TokenPair;
import com.gymmind.shared.security.jwt.TokenType;
import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserInvitationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-13T00:00:00Z");
    private UserInvitationRepository invitations;
    private TenantRepository tenants;
    private UserAccountRepository users;
    private RoleRepository roles;
    private UserRoleRepository userRoles;
    private JwtService jwt;
    private SessionService sessions;
    private UserInvitationService service;

    private static final CurrentActor ADMIN = new CurrentActor(2L, 10L,
            Set.of(RoleCode.GYM_ADMIN), Set.of("user:write"), 0L, "admin-token");

    @BeforeEach
    void setUp() {
        invitations = mock(UserInvitationRepository.class);
        tenants = mock(TenantRepository.class);
        users = mock(UserAccountRepository.class);
        roles = mock(RoleRepository.class);
        userRoles = mock(UserRoleRepository.class);
        jwt = mock(JwtService.class);
        sessions = new SessionService(mock(com.gymmind.iam.application.port.SessionStore.class),
                Clock.fixed(NOW, ZoneOffset.UTC), "test");
        service = new DefaultUserInvitationService(invitations, tenants, users, roles, userRoles, jwt, sessions,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void gymAdminInvitationReturnsRawTokenOnlyOnIssue() {
        when(users.findByNormalizedEmail("member@example.com")).thenReturn(Optional.empty());
        when(invitations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserInvitationService.InvitationIssued issued = service.invite(ADMIN,
                new InviteUserCommand(" member@example.com ", RoleCode.MEMBER));

        assertThat(issued.rawToken()).isNotBlank();
        assertThat(issued.tenantId()).isEqualTo(10L);
        assertThat(issued.role()).isEqualTo(RoleCode.MEMBER);
    }

    @Test
    void invitationCanBeAcceptedOnlyOnce() {
        UserInvitation invitation = UserInvitation.issue(10L, "member@example.com", RoleCode.MEMBER,
                UserInvitation.sha256("invite-token"), NOW.plusSeconds(3600));
        ReflectionTestUtils.setField(invitation, "id", 30L);
        Tenant tenant = Tenant.create("gym", "Gym");
        ReflectionTestUtils.setField(tenant, "id", 10L);
        Role role = Role.of(RoleCode.MEMBER, "Member");
        ReflectionTestUtils.setField(role, "id", 4L);
        UserAccount user = UserAccount.tenantUser(10L, "member@example.com", "hash", "Member", RoleCode.MEMBER);
        ReflectionTestUtils.setField(user, "id", 40L);
        when(invitations.findByTokenHash(UserInvitation.sha256("invite-token"))).thenReturn(Optional.of(invitation));
        when(tenants.findById(10L)).thenReturn(Optional.of(tenant));
        when(users.findByNormalizedEmail("member@example.com")).thenReturn(Optional.empty());
        when(users.save(any())).thenReturn(user);
        when(roles.findByCode(RoleCode.MEMBER)).thenReturn(Optional.of(role));
        when(userRoles.findRoleCodesByUserId(40L)).thenReturn(Set.of(RoleCode.MEMBER));
        when(userRoles.findPermissionCodesByUserId(40L)).thenReturn(Set.of());
        when(jwt.issue(any())).thenReturn(new TokenPair("access", NOW.plusSeconds(900), "refresh", NOW.plusSeconds(3600)));
        when(jwt.verify("refresh", TokenType.REFRESH)).thenReturn(new JwtClaims("GymMind", 40L, 10L,
                Set.of(RoleCode.MEMBER), Set.of(), 0L, "refresh-id", TokenType.REFRESH, NOW, NOW.plusSeconds(3600)));

        service.accept(new AcceptInvitationCommand("invite-token", "ValidPassword123!", "Member"));

        assertThatThrownBy(() -> service.accept(
                new AcceptInvitationCommand("invite-token", "AnotherPassword123!", "Member")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void platformAdminCannotIssueInvitationAndExpiredInvitationIsRejected() {
        CurrentActor platform = new CurrentActor(1L, null, Set.of(RoleCode.PLATFORM_ADMIN),
                Set.of("platform:tenant:write"), 0L, "platform-token");
        assertThatThrownBy(() -> service.invite(platform,
                new InviteUserCommand("member@example.com", RoleCode.MEMBER)))
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);

        UserInvitation expired = UserInvitation.issue(10L, "expired@example.com", RoleCode.MEMBER,
                UserInvitation.sha256("expired"), NOW.minusSeconds(1));
        when(invitations.findByTokenHash(UserInvitation.sha256("expired"))).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.accept(
                new AcceptInvitationCommand("expired", "ValidPassword123!", "Expired")))
                .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHENTICATED);
    }
}
