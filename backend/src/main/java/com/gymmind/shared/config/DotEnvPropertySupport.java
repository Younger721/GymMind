package com.gymmind.shared.config;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 从仓库根目录解析 `.env` 行（KEY=VALUE），供 Spring 环境后处理器使用。 */
public final class DotEnvPropertySupport {

    private DotEnvPropertySupport() {
    }

    /**
     * 定位仓库根 {@code .env}：从 {@code backend} 模块启动时优先 {@code ../.env}，避免误用空的 {@code backend/.env}。
     */
    public static Path locateEnvFile() {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path repoRoot = isBackendModuleDirectory(cwd) ? cwd.getParent() : cwd;
        if (repoRoot != null) {
            Path rootEnv = repoRoot.resolve(".env");
            if (Files.isRegularFile(rootEnv)) {
                return rootEnv;
            }
        }
        Path localEnv = cwd.resolve(".env");
        if (Files.isRegularFile(localEnv)) {
            return localEnv;
        }
        for (Path dir = cwd; dir != null; dir = dir.getParent()) {
            if (repoRoot != null && dir.equals(repoRoot)) {
                continue;
            }
            Path candidate = dir.resolve(".env");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            if (dir.getParent() == null || dir.equals(dir.getRoot())) {
                break;
            }
        }
        return null;
    }

    private static boolean isBackendModuleDirectory(Path cwd) {
        return cwd != null
                && "backend".equalsIgnoreCase(cwd.getFileName().toString())
                && Files.isRegularFile(cwd.resolve("pom.xml"));
    }

    public static Map<String, String> parseEnvLines(List<String> lines) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                value = value.substring(1, value.length() - 1);
            }
            if (!key.isEmpty()) {
                out.put(key, value);
            }
        }
        return out;
    }

    public static boolean isNonBlank(String value) {
        return value != null && !value.isBlank();
    }

    /** 仅看 OS/IDE 的 {@code systemEnvironment}；空字符串视为未设置，便于 .env 补全。 */
    public static boolean hasNonBlankSystemEnvironmentValue(ConfigurableEnvironment environment, String key) {
        if (environment == null || key == null || key.isBlank()) {
            return false;
        }
        PropertySource<?> systemEnvironment = environment.getPropertySources().get("systemEnvironment");
        if (systemEnvironment == null) {
            return false;
        }
        Object raw = systemEnvironment.getProperty(key);
        return raw != null && !raw.toString().isBlank();
    }
}
