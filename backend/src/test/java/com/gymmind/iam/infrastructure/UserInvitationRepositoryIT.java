package com.gymmind.iam.infrastructure;

import com.gymmind.iam.domain.model.InvitationStatus;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserInvitation;
import com.gymmind.iam.domain.repository.UserInvitationRepository;
import com.gymmind.support.MySqlIntegrationTest;
import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class UserInvitationRepositoryIT extends MySqlIntegrationTest {

    @Autowired
    private UserInvitationRepository invitationRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void savesAndFindsInvitationByTokenHash() {
        Long tenantId = tenantRepository.save(Tenant.create("invite-gym", "Invite Gym")).getId();
        UserInvitation invitation = invitationRepository.save(UserInvitation.issue(
                tenantId,
                " Member@Example.COM ",
                RoleCode.MEMBER,
                UserInvitation.sha256("raw-invitation-token"),
                Instant.parse("2026-09-14T00:00:00Z")));
        entityManager.flush();
        entityManager.clear();

        UserInvitation loaded = invitationRepository.findByTokenHash(
                        UserInvitation.sha256("raw-invitation-token"))
                .orElseThrow();
        assertThat(loaded.getId()).isEqualTo(invitation.getId());
        assertThat(loaded.getTenantId()).isEqualTo(tenantId);
        assertThat(loaded.getEmail()).isEqualTo("member@example.com");
        assertThat(loaded.getStatus()).isEqualTo(InvitationStatus.PENDING);
    }

    @Test
    void tokenHashIsGloballyUniqueAcrossTenantInvitations() {
        Long firstTenantId = tenantRepository.save(Tenant.create("first-invite-gym", "First Invite Gym")).getId();
        Long secondTenantId = tenantRepository.save(Tenant.create("second-invite-gym", "Second Invite Gym")).getId();
        String tokenHash = UserInvitation.sha256("same-raw-token");

        invitationRepository.save(UserInvitation.issue(
                firstTenantId, "first@example.com", RoleCode.COACH, tokenHash,
                Instant.parse("2026-09-14T00:00:00Z")));
        entityManager.flush();

        invitationRepository.save(UserInvitation.issue(
                secondTenantId, "second@example.com", RoleCode.MEMBER, tokenHash,
                Instant.parse("2026-09-14T00:00:00Z")));
        assertThatThrownBy(() -> entityManager.flush())
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
