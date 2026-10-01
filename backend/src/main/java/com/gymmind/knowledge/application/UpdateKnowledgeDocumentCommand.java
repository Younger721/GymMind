package com.gymmind.knowledge.application;

import com.gymmind.knowledge.domain.model.DocumentVisibility;
import com.gymmind.iam.domain.model.RoleCode;
import java.util.List;

/** 更新知识库文档元数据（不含文件内容替换） */
public record UpdateKnowledgeDocumentCommand(DocumentVisibility visibility, List<RoleCode> audienceRoles) {
    public UpdateKnowledgeDocumentCommand(DocumentVisibility visibility) { this(visibility, null); }
}
