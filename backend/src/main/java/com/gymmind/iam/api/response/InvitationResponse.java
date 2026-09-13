package com.gymmind.iam.api.response;

import com.gymmind.iam.application.UserInvitationService;
import com.gymmind.iam.domain.model.RoleCode;

import java.time.Instant;

public record InvitationResponse(String rawToken, Long tenantId, String email,
                                 RoleCode role, Instant expiresAt) {
    public static InvitationResponse from(UserInvitationService.InvitationIssued issued) {
        return new InvitationResponse(issued.rawToken(), issued.tenantId(), issued.email(), issued.role(), issued.expiresAt());
    }
}
