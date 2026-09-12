package com.gymmind.iam.application.command;

public record LogoutCommand(String accessToken, String refreshToken) {
}
