package com.gymmind.knowledge.application;

import java.util.Map;

/** 前端加载 OnlyOffice 所需信息 */
public record KnowledgeOnlyOfficeEditorView(
        String documentServerUrl,
        String token,
        /** 与 JWT payload 一致，须与 token 一并传给 DocEditor */
        Map<String, Object> config,
        Long documentId,
        String fileName) {
}
