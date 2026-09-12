package com.gymmind.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityPropertiesTest {

    @Test
    void rejectsJwtSecretShorterThan256Bits() {
        var jwt = new SecurityProperties.Jwt("gymmind", "too-short", Duration.ofMinutes(15), Duration.ofDays(7));

        assertThatThrownBy(jwt::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256");
    }

    @Test
    void measuresJwtSecretLengthUsingUtf8Bytes() {
        var thirtyBytes = new SecurityProperties.Jwt("gymmind", "中".repeat(10), Duration.ofMinutes(15), Duration.ofDays(7));
        var thirtyThreeBytes = new SecurityProperties.Jwt("gymmind", "中".repeat(11), Duration.ofMinutes(15), Duration.ofDays(7));

        assertThatThrownBy(thirtyBytes::validate).isInstanceOf(IllegalStateException.class);
        thirtyThreeBytes.validate();
    }

    @Test
    void validatesBoundPropertiesDuringApplicationStartup() {
        new ApplicationContextRunner()
                .withUserConfiguration(BoundSecurityProperties.class)
                .withPropertyValues(
                        "gymmind.security.jwt.issuer=GymMind",
                        "gymmind.security.jwt.secret=short-value",
                        "gymmind.security.jwt.access-ttl=PT15M",
                        "gymmind.security.jwt.refresh-ttl=P7D")
                .run(context -> assertThat(context.getStartupFailure())
                        .hasRootCauseInstanceOf(IllegalStateException.class)
                        .hasRootCauseMessage("JWT secret must be at least 256 bits (32 UTF-8 bytes)"));
    }

    @EnableConfigurationProperties(SecurityProperties.class)
    static class BoundSecurityProperties {
    }
}
