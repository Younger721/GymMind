package com.gymmind.shared.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将仓库根 {@code .env} 合并进 Spring {@link ConfigurableEnvironment}。
 * 非空的 OS/IDE 环境变量优先；空字符串占位不视为已配置，由 .env 补全（避免 IDEA 空变量挡住根目录密钥）。
 */
public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DotEnvEnvironmentPostProcessor.class);
    static final String PROPERTY_SOURCE_NAME = "gymMindDotEnv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path envFile = DotEnvPropertySupport.locateEnvFile();
        if (envFile == null) {
            return;
        }
        mergeEnvFile(envFile, environment);
    }

    /** 供单测验证合并规则；生产路径由 {@link #postProcessEnvironment} 调用。 */
    static void mergeEnvFile(Path envFile, ConfigurableEnvironment environment) {
        try {
            Map<String, String> fromFile = DotEnvPropertySupport.parseEnvLines(
                    Files.readAllLines(envFile, StandardCharsets.UTF_8));
            Map<String, Object> toAdd = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : fromFile.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (value.isBlank() || isDeprecatedEnvKey(key)) {
                    continue;
                }
                if (DotEnvPropertySupport.hasNonBlankSystemEnvironmentValue(environment, key)) {
                    continue;
                }
                toAdd.put(key, value);
            }
            if (toAdd.isEmpty()) {
                return;
            }
            MutablePropertySources sources = environment.getPropertySources();
            PropertySource<?> dotEnv = new MapPropertySource(PROPERTY_SOURCE_NAME, toAdd);
            // 置于最前，使 .env 能覆盖 systemEnvironment 中的空字符串占位
            sources.addFirst(dotEnv);
            log.info("已合并 {} 项 .env 配置到 Spring 环境（{}，不覆盖非空 OS/IDE 环境变量）", toAdd.size(), envFile);
        } catch (IOException exception) {
            log.warn("读取 .env 失败: {} ({})", envFile, exception.getMessage());
        }
    }

    /** 已废弃的 DeepSeek 变量，不再从 .env 注入 Spring。 */
    private static boolean isDeprecatedEnvKey(String key) {
        return "AI_API_KEY".equals(key)
                || "AI_API_BASE_URL".equals(key)
                || "EMBEDDING_MODEL".equals(key);
    }
}
