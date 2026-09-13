package com.gymmind.iam.api.request;

import com.gymmind.iam.domain.model.UserStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeUserStatusRequest(@NotNull UserStatus status) {
}
