package com.gymmind.platform.database;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseResetScriptContractTest {

    @Test
    void resetScriptsUseOnlyFixedGymmindSqlAndExplicitConfirmation() throws IOException {
        String powershell = Files.readString(script("reset-gymmind-db.ps1"));
        String shell = Files.readString(script("reset-gymmind-db.sh"));

        for (String content : new String[]{powershell, shell}) {
            assertThat(content).contains("DROP DATABASE IF EXISTS gymmind");
            assertThat(content).contains("CREATE DATABASE gymmind CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
            assertThat(content).contains("DROP gymmind");
            assertThat(content).doesNotContain("DROP DATABASE IF EXISTS $database");
            assertThat(content).doesNotContain("DROP DATABASE IF EXISTS ${");
            assertThat(content).doesNotContain("DROP DATABASE IF EXISTS `");
        }
    }

    @Test
    void resetScriptsAreDryRunByDefault() throws IOException {
        assertThat(Files.readString(script("reset-gymmind-db.ps1")))
                .contains("[switch]$Execute");
        assertThat(Files.readString(script("reset-gymmind-db.sh")))
                .contains("--execute");
    }

    private static Path script(String name) {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path fromRoot = current.resolve("backend").resolve("scripts").resolve(name);
        return Files.exists(fromRoot) ? fromRoot : current.resolve("scripts").resolve(name);
    }
}
