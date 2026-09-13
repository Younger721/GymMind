package com.gymmind.iam.application;

import com.gymmind.iam.application.command.AcceptInvitationCommand;
import com.gymmind.iam.application.command.InviteUserCommand;
import com.gymmind.iam.application.result.AuthResult;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;

import java.time.Instant;

public interface UserInvitationService {
    InvitationIssued invite(CurrentActor actor, InviteUserCommand command);
    AuthResult accept(AcceptInvitationCommand command);

    record InvitationIssued(String rawToken, Long tenantId, String email,
                            RoleCode role, Instant expiresAt) {}
}
