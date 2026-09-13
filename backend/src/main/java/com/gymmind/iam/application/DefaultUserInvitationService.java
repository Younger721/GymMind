package com.gymmind.iam.application;

import com.gymmind.iam.application.command.AcceptInvitationCommand;
import com.gymmind.iam.application.command.InviteUserCommand;
import com.gymmind.iam.application.result.AuthResult;
import com.gymmind.iam.domain.model.Role;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.model.UserInvitation;
import com.gymmind.iam.domain.model.InvitationStatus;
import com.gymmind.iam.domain.repository.RoleRepository;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.iam.domain.repository.UserInvitationRepository;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.jwt.JwtService;
import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;

@Service
public class DefaultUserInvitationService implements UserInvitationService {

    private final UserInvitationRepository invitations;
    private final TenantRepository tenants;
    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final UserRoleRepository userRoles;
    private final JwtService jwt;
    private final SessionService sessions;
    private final Clock clock;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public DefaultUserInvitationService(UserInvitationRepository invitations, TenantRepository tenants,
                                        UserAccountRepository users, RoleRepository roles,
                                        UserRoleRepository userRoles, JwtService jwt, SessionService sessions,
                                        Clock clock) {
        this.invitations = Objects.requireNonNull(invitations, "invitations");
        this.tenants = Objects.requireNonNull(tenants, "tenants");
        this.users = Objects.requireNonNull(users, "users");
        this.roles = Objects.requireNonNull(roles, "roles");
        this.userRoles = Objects.requireNonNull(userRoles, "userRoles");
        this.jwt = Objects.requireNonNull(jwt, "jwt");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    @Transactional
    public InvitationIssued invite(CurrentActor actor, InviteUserCommand command) {
        requireGymAdmin(actor, "user:write");
        Objects.requireNonNull(command, "command");
        RoleCode role = command.role();
        if (role != RoleCode.COACH && role != RoleCode.MEMBER) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        String email = UserAccount.normalizeEmail(command.email());
        if (users.findByNormalizedEmail(email).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        String rawToken = randomToken();
        Instant expiresAt = clock.instant().plus(Duration.ofHours(24));
        UserInvitation saved = invitations.save(UserInvitation.issue(
                actor.tenantId(), email, role, UserInvitation.sha256(rawToken), expiresAt));
        return new InvitationIssued(rawToken, saved.getTenantId(), saved.getEmail(), saved.getRole(), saved.getExpiresAt());
    }

    @Override
    @Transactional
    public AuthResult accept(AcceptInvitationCommand command) {
        Objects.requireNonNull(command, "command");
        String token = required(command.token(), "Invitation token");
        UserInvitation invitation = invitations.findByTokenHash(UserInvitation.sha256(token))
                .orElseThrow(DefaultUserInvitationService::unauthenticated);
        Instant now = clock.instant();
        if (invitation.getStatus() == InvitationStatus.ACCEPTED) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        if (!invitation.isAcceptable(now)) {
            throw unauthenticated();
        }
        Tenant tenant = tenants.findById(invitation.getTenantId())
                .filter(Tenant::isActive).orElseThrow(DefaultUserInvitationService::unauthenticated);
        if (users.findByNormalizedEmail(invitation.getEmail()).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        UserAccount user = users.save(UserAccount.tenantUser(invitation.getTenantId(), invitation.getEmail(),
                passwordEncoder.encode(required(command.password(), "Password")),
                required(command.displayName(), "Display name"), invitation.getRole()));
        Role role = roles.findByCode(invitation.getRole()).orElseThrow(DefaultUserInvitationService::unauthenticated);
        userRoles.save(com.gymmind.iam.domain.model.UserRole.assign(user, role));
        invitation.accept(now);
        invitations.save(invitation);
        Set<RoleCode> roleCodes = userRoles.findRoleCodesByUserId(user.getId());
        Set<String> permissions = userRoles.findPermissionCodesByUserId(user.getId());
        CurrentActor actor = new CurrentActor(user.getId(), tenant.getId(), roleCodes, permissions,
                user.getTokenVersion(), "invitation-accept");
        com.gymmind.shared.security.jwt.TokenPair pair = jwt.issue(actor);
        var refreshClaims = jwt.verify(pair.refreshToken(), com.gymmind.shared.security.jwt.TokenType.REFRESH);
        sessions.store(sessions.refreshSession(refreshClaims.tenantId(), refreshClaims.userId(),
                refreshClaims.tokenId(), refreshClaims.expiresAt()), pair.refreshToken());
        return new AuthResult(new AuthResult.AuthenticatedUser(user.getId(), user.getTenantId(),
                user.getNormalizedEmail(), user.getDisplayName(), roleCodes, permissions), pair);
    }

    private static void requireGymAdmin(CurrentActor actor, String permission) {
        if (actor == null || actor.tenantId() == null || !actor.roles().contains(RoleCode.GYM_ADMIN)
                || !actor.hasPermission(permission)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, label + " must not be blank");
        }
        return value.trim();
    }

    private static BusinessException unauthenticated() {
        return new BusinessException(ErrorCode.UNAUTHENTICATED);
    }
}
