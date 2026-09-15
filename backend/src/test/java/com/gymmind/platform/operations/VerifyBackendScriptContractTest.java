package com.gymmind.platform.operations;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class VerifyBackendScriptContractTest {
    @Test
    void verificationScriptUsesJava17AndHealthEndpointWithoutSecrets() throws Exception {
        Path current = Path.of(System.getProperty("user.dir"));
        Path script = current.resolve("scripts").resolve("verify-backend.ps1");
        if (!Files.exists(script)) script = current.getParent().resolve("backend").resolve("scripts").resolve("verify-backend.ps1");
        String content = Files.readString(script);
        assertThat(content).contains("JAVA_HOME");
        assertThat(content).contains("mvnw.cmd");
        assertThat(content).contains("actuator/health");
        assertThat(content).doesNotContain("sk-");
        assertThat(content).doesNotContain("MYSQL_PASSWORD=");
    }
}
