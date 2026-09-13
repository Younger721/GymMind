package com.gymmind.iam.infrastructure.persistence;

import com.gymmind.iam.domain.model.UserInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface SpringDataUserInvitationRepository extends JpaRepository<UserInvitation, Long> {
    Optional<UserInvitation> findByTokenHash(String tokenHash);
}
