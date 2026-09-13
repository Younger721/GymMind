package com.gymmind.knowledge.application;
import com.gymmind.knowledge.domain.model.DocumentVisibility;
public record UploadKnowledgeDocumentCommand(String fileName, String contentType, byte[] content, DocumentVisibility visibility) {}
