package com.gymmind.knowledge.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymmind.knowledge.config.KnowledgeOnlyOfficeProperties;
import com.gymmind.knowledge.domain.model.DocumentStatus;
import com.gymmind.knowledge.domain.model.KnowledgeDocument;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.TenantContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class KnowledgeOnlyOfficeService {

    private final KnowledgeOnlyOfficeProperties properties;
    private final KnowledgeDocumentUseCase documents;
    private final KnowledgeEditorAccessTokenService tokens;
    private final OnlyOfficeJwtSigner jwtSigner;
    private final KnowledgeEditorActorResolver actorResolver;
    private final RestClient restClient;

    public KnowledgeOnlyOfficeService(
            KnowledgeOnlyOfficeProperties properties,
            KnowledgeDocumentUseCase documents,
            KnowledgeEditorAccessTokenService tokens,
            OnlyOfficeJwtSigner jwtSigner,
            KnowledgeEditorActorResolver actorResolver,
            RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.documents = documents;
        this.tokens = tokens;
        this.jwtSigner = jwtSigner;
        this.actorResolver = actorResolver;
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public KnowledgeOnlyOfficeEditorView buildEditorConfig(CurrentActor actor, long documentId) {
        if (!properties.isEnabled()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        KnowledgeDocumentView view = documents.find(actor, documentId);
        if (!isDocxFile(view.fileName())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (isIngestBusy(view.status())) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        long tenantId = ActorAccess.requireTenantId(actor);
        String downloadToken = tokens.issueDownloadToken(documentId, actor.userId(), tenantId);
        boolean editable = ActorAccess.canManageTenant(actor);
        String apiBase = properties.normalizedInternalApiBaseUrl();
        String documentUrl = apiBase + "/api/v1/knowledge/documents/" + documentId
                + "/content?token=" + urlEncode(downloadToken) + "&userId=" + actor.userId();

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("fileType", "docx");
        document.put("key", documentId + "-" + Instant.now().toEpochMilli());
        document.put("title", view.fileName());
        document.put("url", documentUrl);
        document.put("permissions", Map.of(
                "edit", editable,
                "download", true,
                "print", true,
                "review", true,
                "comment", true,
                "fillForms", true,
                "copy", true));

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", String.valueOf(actor.userId()));
        user.put("name", "用户" + actor.userId());

        Map<String, Object> coEditing = new LinkedHashMap<>();
        coEditing.put("mode", "fast");
        coEditing.put("change", true);

        Map<String, Object> editorConfig = new LinkedHashMap<>();
        editorConfig.put("lang", "zh-CN");
        editorConfig.put("mode", editable ? "edit" : "view");
        if (editable) {
            String callbackToken = tokens.issueCallbackToken(documentId, actor.userId(), tenantId);
            String callbackUrl = apiBase + "/api/v1/knowledge/documents/" + documentId
                    + "/onlyoffice/callback?userId=" + actor.userId() + "&session=" + urlEncode(callbackToken);
            editorConfig.put("callbackUrl", callbackUrl);
            editorConfig.put("coEditing", coEditing);
        }
        editorConfig.put("user", user);

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("document", document);
        config.put("documentType", "word");
        config.put("editorConfig", editorConfig);

        String signed = jwtSigner.signConfig(config);
        return new KnowledgeOnlyOfficeEditorView(
                properties.normalizedDocumentServerUrl(),
                signed,
                config,
                documentId,
                view.fileName());
    }

    public byte[] readContentForOnlyOffice(long documentId, long userId, String token) {
        long tenantId = tokens.verifyDownloadToken(documentId, userId, token);
        Long previousTenant = TenantContextHolder.get();
        TenantContextHolder.set(tenantId);
        try {
            CurrentActor actor = actorResolver.requireActiveEditorActor(userId);
            if (actor.tenantId() != null && !actor.tenantId().equals(tenantId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            return documents.readBinaryContent(actor, documentId);
        } finally {
            if (previousTenant == null) TenantContextHolder.clear(); else TenantContextHolder.set(previousTenant);
        }
    }

    public Map<String, Object> handleCallback(long documentId, long userId, String sessionToken, JsonNode body) {
        long tenantId = tokens.verifyCallbackToken(documentId, userId, sessionToken);
        String callbackKey = jwtSigner.verifyCallbackToken(
                body == null ? null : body.path("token").asText(null), documentId);
        if (!callbackKey.equals(body.path("key").asText(null))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        int status = body.path("status").asInt(-1);
        if (status != 2 && status != 6) {
            return Map.of("error", 0);
        }
        String downloadUrl = body.path("url").asText(null);
        if (downloadUrl == null || downloadUrl.isBlank()) {
            return Map.of("error", 1);
        }
        URI callbackUri = validateCallbackUrl(downloadUrl, properties);
        byte[] bytes = restClient.get()
                .uri(callbackUri)
                .exchange((request, response) -> readBoundedResponse(response));
        if (bytes == null || bytes.length == 0) {
            return Map.of("error", 1);
        }
        CurrentActor actor = actorResolver.requireActiveEditorActor(userId);
        boolean platformAdmin = actor.tenantId() == null;
        if (platformAdmin) {
            if (!ActorAccess.isSuperAdmin(actor)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            TenantContextHolder.set(tenantId);
        } else if (!actor.tenantId().equals(tenantId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        try {
            documents.replaceBinaryContent(actor, documentId, bytes);
        } finally {
            if (platformAdmin) {
                TenantContextHolder.clear();
            }
        }
        return Map.of("error", 0);
    }

    static URI validateCallbackUrl(String rawUrl, KnowledgeOnlyOfficeProperties properties) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        try {
            URI uri = new URI(rawUrl.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            boolean hostAllowed = properties.getCallbackAllowedHosts().stream()
                    .map(value -> value == null ? "" : value.trim().toLowerCase(Locale.ROOT))
                    .anyMatch(host::equals);
            int port = uri.getPort() == -1 ? ("https".equals(scheme) ? 443 : 80) : uri.getPort();
            if ((!"http".equals(scheme) && !"https".equals(scheme))
                    || uri.getUserInfo() != null
                    || host.isBlank()
                    || !hostAllowed
                    || !properties.getCallbackAllowedPorts().contains(port)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            return uri;
        } catch (URISyntaxException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    private byte[] readBoundedResponse(org.springframework.http.client.ClientHttpResponse response) throws IOException {
        HttpStatusCode status = response.getStatusCode();
        if (!status.is2xxSuccessful()) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
        long maxBytes = properties.getCallbackMaxBytes();
        if (maxBytes <= 0 || (response.getHeaders().getContentLength() > maxBytes)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        try (var input = response.getBody()) {
            return readBounded(input, maxBytes);
        }
    }

    static byte[] readBounded(java.io.InputStream input, long maxBytes) throws IOException {
        try (var output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED);
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static boolean isDocxFile(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".docx");
    }

    private static boolean isIngestBusy(DocumentStatus status) {
        return status == DocumentStatus.PARSING
                || status == DocumentStatus.CHUNKED
                || status == DocumentStatus.INDEXING
                || status == DocumentStatus.UPLOADING;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
