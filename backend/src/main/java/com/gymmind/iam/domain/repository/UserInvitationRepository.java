package com.gymmind.iam.domain.repository;

import com.gymmind.iam.domain.model.UserInvitation;

import java.util.Optional;

public interface UserInvitationRepository {
    UserInvitation save(UserInvitation invitation);
    Optional<UserInvitation> findByTokenHash(String tokenHash);
}
