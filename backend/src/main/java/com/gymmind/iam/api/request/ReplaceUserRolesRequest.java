package com.gymmind.iam.api.request;

import com.gymmind.iam.domain.model.RoleCode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record ReplaceUserRolesRequest(@NotEmpty Set<@NotNull RoleCode> roles) {
}
