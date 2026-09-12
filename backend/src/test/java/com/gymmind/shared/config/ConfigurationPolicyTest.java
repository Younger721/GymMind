package com.gymmind.shared.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ConfigurationPolicyTest {
    private static final Pattern SCHEMA = Pattern.compile("jdbc:mysql://[^/]+/([^?]+)");
    private static final Pattern SECRET_PROPERTY = Pattern.compile("(?im)^\\s*(?:password|secret|secret-key|api-key):\\s*(.+?)\\s*$");

    @Test
    void allEnvironmentProfilesUseGymmindAndTheirDeclaredDdlPolicies() throws IOException {
        Map<String, Object> base = yaml("application.yml");
        Map<String, Object> dev = yaml("application-dev.yml");
        Map<String, Object> test = yaml("application-test.yml");
        Map<String, Object> prod = yaml("application-prod.yml");

        assertThat(schema(dev)).isEqualTo("gymmind");
        assertThat(schema(test)).isEqualTo("gymmind");
        assertThat(schema(prod)).isEqualTo("gymmind");
        assertThat(value(dev, "spring", "jpa", "hibernate", "ddl-auto")).isEqualTo("update");
        assertThat(value(test, "spring", "jpa", "hibernate", "ddl-auto")).isEqualTo("create-drop");
        assertThat(value(prod, "spring", "jpa", "hibernate", "ddl-auto")).isEqualTo("validate");
        assertThat(value(base, "spring", "profiles", "default")).isEqualTo("dev");
    }

    @Test
    void developmentAndTestDatabaseNamesCannotBeOverridden() throws IOException {
        assertThat(text("application-dev.yml")).contains("/gymmind?").doesNotContain("MYSQL_DATABASE");
        assertThat(text("application-test.yml")).contains("/gymmind?").doesNotContain("MYSQL_DATABASE");
    }

    @Test
    void productionDatabaseAndSecretsRequireEnvironmentValues() throws IOException {
        String production = text("application-prod.yml");
        assertThat(production).contains("${MYSQL_HOST}", "${MYSQL_PORT}", "${MYSQL_USER}", "${MYSQL_PASSWORD}");
        assertThat(production).contains("${JWT_SECRET}");
    }

    @Test
    void resourceFilesContainNoProviderKeyPrefixesOrNonPlaceholderSecrets() throws IOException {
        String forbiddenPrefix = "s" + "k-";
        try (var paths = Files.walk(Path.of("src/main/resources"))) {
            paths.filter(Files::isRegularFile).forEach(path -> assertThatCode(() -> {
                String resource = Files.readString(path, StandardCharsets.UTF_8);
                assertThat(resource).doesNotContain(forbiddenPrefix);
                Matcher matcher = SECRET_PROPERTY.matcher(resource);
                while (matcher.find()) {
                    String value = matcher.group(1).replace("'", "").replace("\"", "").trim();
                    assertThat(value).as("secret in %s", path).satisfies(v ->
                            assertThat(v.isBlank() || v.contains("${") || v.startsWith("<") || v.startsWith("test-"))
                                    .isTrue());
                }
            }).doesNotThrowAnyException());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> yaml(String name) throws IOException {
        return new Yaml().load(text(name));
    }

    private String text(String name) throws IOException {
        return Files.readString(Path.of("src/main/resources", name), StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private String schema(Map<String, Object> yaml) {
        Matcher matcher = SCHEMA.matcher((String) value(yaml, "spring", "datasource", "url"));
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    @SuppressWarnings("unchecked")
    private Object value(Map<String, Object> root, String... keys) {
        Object current = root;
        for (String key : keys) {
            current = ((Map<String, Object>) current).get(key);
        }
        return current;
    }
}
