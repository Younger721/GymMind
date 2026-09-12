package com.gymmind.shared.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "gymmind.security")
public class SecurityProperties {
    private Jwt jwt = new Jwt("GymMind", "", Duration.ofMinutes(15), Duration.ofDays(7));

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    @PostConstruct
    void validateOnStartup() {
        jwt.validate();
    }

    public record Jwt(String issuer, String secret, Duration accessTtl, Duration refreshTtl) {
        public void validate() {
            int secretBytes = secret == null ? 0 : secret.getBytes(StandardCharsets.UTF_8).length;
            if (secretBytes < 32) {
                throw new IllegalStateException("JWT secret must be at least 256 bits (32 UTF-8 bytes)");
            }
        }
    }
}
