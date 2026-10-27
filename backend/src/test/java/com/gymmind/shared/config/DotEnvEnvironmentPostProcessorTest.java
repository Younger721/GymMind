package com.gymmind.shared.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DotEnvEnvironmentPostProcessorTest {

    @Test
    void mergeEnvFileOverridesBlankSystemEnvironment(@TempDir Path tempDir) throws Exception {
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, "DASHSCOPE_API_KEY=from-dotenv\n");

        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("systemEnvironment",
                java.util.Map.of("DASHSCOPE_API_KEY", "")));

        DotEnvEnvironmentPostProcessor.mergeEnvFile(envFile, environment);

        assertThat(environment.getProperty("DASHSCOPE_API_KEY")).isEqualTo("from-dotenv");
    }

    @Test
    void mergeEnvFileDoesNotOverrideNonBlankSystemEnvironment(@TempDir Path tempDir) throws Exception {
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, "DASHSCOPE_API_KEY=from-dotenv\n");

        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("systemEnvironment",
                java.util.Map.of("DASHSCOPE_API_KEY", "from-os")));

        DotEnvEnvironmentPostProcessor.mergeEnvFile(envFile, environment);

        assertThat(environment.getProperty("DASHSCOPE_API_KEY")).isEqualTo("from-os");
    }
}
