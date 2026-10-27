package com.gymmind.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "gymmind.knowledge.onlyoffice")
public class KnowledgeOnlyOfficeProperties {

    private boolean enabled;
    private String documentServerUrl = "http://localhost:8088";
    /** Document Server 容器访问 Spring Boot 的基址 */
    private String internalApiBaseUrl = "http://host.docker.internal:8080";
    private String jwtSecret = "GymMindOnlyOfficeDevSecretChangeMe32";
    private Duration downloadTokenTtl = Duration.ofMinutes(15);
    private List<String> callbackAllowedHosts = new ArrayList<>(List.of("onlyoffice"));
    private List<Integer> callbackAllowedPorts = new ArrayList<>(List.of(80));
    private long callbackMaxBytes = 25L * 1024 * 1024;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDocumentServerUrl() {
        return documentServerUrl;
    }

    public void setDocumentServerUrl(String documentServerUrl) {
        this.documentServerUrl = documentServerUrl;
    }

    public String getInternalApiBaseUrl() {
        return internalApiBaseUrl;
    }

    public void setInternalApiBaseUrl(String internalApiBaseUrl) {
        this.internalApiBaseUrl = internalApiBaseUrl;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public Duration getDownloadTokenTtl() {
        return downloadTokenTtl;
    }

    public void setDownloadTokenTtl(Duration downloadTokenTtl) {
        this.downloadTokenTtl = downloadTokenTtl;
    }

    public List<String> getCallbackAllowedHosts() {
        return callbackAllowedHosts;
    }

    public void setCallbackAllowedHosts(List<String> callbackAllowedHosts) {
        this.callbackAllowedHosts = callbackAllowedHosts == null ? List.of() : List.copyOf(callbackAllowedHosts);
    }

    public List<Integer> getCallbackAllowedPorts() {
        return callbackAllowedPorts;
    }

    public void setCallbackAllowedPorts(List<Integer> callbackAllowedPorts) {
        this.callbackAllowedPorts = callbackAllowedPorts == null ? List.of() : List.copyOf(callbackAllowedPorts);
    }

    public long getCallbackMaxBytes() {
        return callbackMaxBytes;
    }

    public void setCallbackMaxBytes(long callbackMaxBytes) {
        this.callbackMaxBytes = callbackMaxBytes;
    }

    public String normalizedDocumentServerUrl() {
        return trimTrailingSlash(documentServerUrl);
    }

    public String normalizedInternalApiBaseUrl() {
        return trimTrailingSlash(internalApiBaseUrl);
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
