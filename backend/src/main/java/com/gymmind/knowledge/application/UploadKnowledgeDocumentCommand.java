package com.gymmind.knowledge.application;
import com.gymmind.knowledge.domain.model.DocumentVisibility;
import com.gymmind.iam.domain.model.RoleCode;
import java.util.List;
public record UploadKnowledgeDocumentCommand(String fileName, String contentType, byte[] content, DocumentVisibility visibility, String category, List<RoleCode> audienceRoles, Long replaceDocumentId) {
    public UploadKnowledgeDocumentCommand(String fileName, String contentType, byte[] content, DocumentVisibility visibility, String category, List<RoleCode> audienceRoles) { this(fileName, contentType, content, visibility, category, audienceRoles, null); }
    public UploadKnowledgeDocumentCommand(String fileName, String contentType, byte[] content, DocumentVisibility visibility, String category) { this(fileName, contentType, content, visibility, category, null, null); }
    public UploadKnowledgeDocumentCommand(String fileName, String contentType, byte[] content, DocumentVisibility visibility) { this(fileName, contentType, content, visibility, "GENERAL", null, null); }
}
