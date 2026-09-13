package com.gymmind.iam.api.request;

import com.gymmind.iam.application.command.AcceptInvitationCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInvitationRequest(@NotBlank String token,
                                      @NotBlank @Size(min = 8, max = 72) String password,
                                      @NotBlank @Size(max = 128) String displayName) {
    public AcceptInvitationCommand toCommand() {
        return new AcceptInvitationCommand(token, password, displayName);
    }
}
